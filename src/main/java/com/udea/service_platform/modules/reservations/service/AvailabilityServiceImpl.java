package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.AvailabilitySlotResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.service.ScheduleLookupService;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final int MINUTOS_POR_DIA = 1440;

    private final ServiceLookupService serviceLookupService;
    private final ScheduleLookupService scheduleLookupService;
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(Long serviceId, LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("La fecha no puede ser en el pasado");
        }

        Service servicio = serviceLookupService.findActiveService(serviceId);

        if (servicio.getDuracion() == null || servicio.getDuracion() <= 0) {
            throw new IllegalArgumentException("La duración del servicio debe ser mayor a cero");
        }

        List<HorarioFranja> schedule = scheduleLookupService.findServiceSchedules(serviceId);
        List<BloqueoFranja> blocks = scheduleLookupService.findProviderBlocks(
                servicio.getIdProveedor(), fecha);

        List<Reserva> reservas = loadActiveReservations(serviceId, fecha);

        List<AvailabilitySlotResponse> slots = buildSlots(fecha, servicio, schedule, blocks, reservas);

        return AvailabilityResponse.builder()
                .fecha(fecha)
                .idServicio(serviceId)
                .duracionMinutos(servicio.getDuracion())
                .slots(slots)
                .build();
    }

    private List<Reserva> loadActiveReservations(Long serviceId, LocalDate fecha) {
        Instant inicioDia = fecha.atStartOfDay(TimeConfig.ZONA_BOGOTA).toInstant();
        Instant inicioSiguiente = fecha.plusDays(1).atStartOfDay(TimeConfig.ZONA_BOGOTA).toInstant();

        return reservaRepository.findActiveReservationsInRange(
                serviceId, EstadoReserva.ACTIVA.getId(), inicioDia, inicioSiguiente);
    }

    private List<AvailabilitySlotResponse> buildSlots(LocalDate fecha, Service servicio,
                                                      List<HorarioFranja> schedule,
                                                      List<BloqueoFranja> blocks,
                                                      List<Reserva> reservas) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        int duracion = servicio.getDuracion();
        boolean hoy = fecha.equals(LocalDate.now(clock));
        int minutoActual = LocalTime.now(clock).toSecondOfDay() / 60;

        List<AvailabilitySlotResponse> slots = new ArrayList<>();

        for (HorarioFranja franja : schedule) {
            if (franja.diaSemana() != diaSemana) {
                continue;
            }
            appendFranjaSlots(slots, franja, duracion, hoy, minutoActual, fecha, blocks, reservas);
        }

        return slots;
    }

    private void appendFranjaSlots(List<AvailabilitySlotResponse> slots, HorarioFranja franja,
                                   int duracion, boolean hoy, int minutoActual, LocalDate fecha,
                                   List<BloqueoFranja> blocks, List<Reserva> reservas) {
        int inicioFranja = aMinutos(franja.horaInicio());
        int finFranja = aMinutos(franja.horaFin());
        if (finFranja <= inicioFranja) {
            // El horario termina al dia siguiente (cruza la medianoche).
            finFranja = finFranja + MINUTOS_POR_DIA;
        }

        int cursor = inicioFranja;
        while (cursor + duracion <= finFranja) {
            int inicioSlot = cursor;
            int finSlot = cursor + duracion;
            cursor = finSlot;

            if (hoy && inicioSlot < minutoActual) {
                continue;
            }

            EstadoSlot estado = resolveEstado(inicioSlot, finSlot, duracion, fecha, blocks, reservas);
            slots.add(AvailabilitySlotResponse.builder()
                    .horaInicio(aHoraLocal(inicioSlot))
                    .horaFin(aHoraLocal(finSlot))
                    .estado(estado)
                    .build());
        }
    }

    private EstadoSlot resolveEstado(int inicio, int fin, int duracion, LocalDate fecha,
                                     List<BloqueoFranja> blocks, List<Reserva> reservas) {
        if (isBlocked(inicio, fin, blocks)) {
            return EstadoSlot.BLOQUEADA;
        }
        if (isOccupied(inicio, duracion, fecha, reservas)) {
            return EstadoSlot.OCUPADA;
        }
        return EstadoSlot.LIBRE;
    }

    private boolean isBlocked(int inicio, int fin, List<BloqueoFranja> blocks) {
        for (BloqueoFranja bloqueo : blocks) {
            if (bloqueo.horaInicio() == null || bloqueo.horaFin() == null) {
                return true;
            }
            int inicioBloqueo = aMinutos(bloqueo.horaInicio());
            int finBloqueo = aMinutos(bloqueo.horaFin());
            if (finBloqueo <= inicioBloqueo) {
                finBloqueo = finBloqueo + MINUTOS_POR_DIA;
            }
            if (inicioBloqueo < fin && finBloqueo > inicio) {
                return true;
            }
        }
        return false;
    }

    private boolean isOccupied(int inicio, int duracion, LocalDate fecha, List<Reserva> reservas) {
        int finSlot = inicio + duracion;

        for (Reserva reserva : reservas) {
            if (reserva.getHoraInicio() == null) {
                continue;
            }
            if (!EstadoReserva.ACTIVA.getId().equals(reserva.getIdEstado())) {
                continue;
            }
            LocalDate fechaReserva = reserva.getHoraInicio().atZone(TimeConfig.ZONA_BOGOTA).toLocalDate();
            if (!fecha.equals(fechaReserva)) {
                continue;
            }
            int inicioReserva = aMinutos(reserva.getHoraInicio().atZone(TimeConfig.ZONA_BOGOTA).toLocalTime());
            int finReserva = inicioReserva + resolveDuracion(reserva, duracion);

            if (inicioReserva < finSlot && finReserva > inicio) {
                return true;
            }
        }
        return false;
    }

    private int resolveDuracion(Reserva reserva, int duracionServicio) {
        if (reserva.getDuracionAcordada() != null) {
            return reserva.getDuracionAcordada();
        }
        return duracionServicio;
    }

    private int aMinutos(LocalTime hora) {
        return hora.toSecondOfDay() / 60;
    }

    private LocalTime aHoraLocal(int minutos) {
        return LocalTime.ofSecondOfDay((minutos % MINUTOS_POR_DIA) * 60L);
    }
}
