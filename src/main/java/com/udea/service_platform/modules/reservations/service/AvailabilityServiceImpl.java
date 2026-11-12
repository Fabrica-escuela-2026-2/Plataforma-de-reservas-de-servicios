package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.AvailabilitySlotResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.dto.RescheduleSlotQuery;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.service.ScheduleLookupService;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

        List<Reserva> reservas = loadOccupyingReservations(serviceId, fecha);

        List<AvailabilitySlotResponse> slots = buildSlots(fecha, servicio, schedule, blocks, reservas);

        return AvailabilityResponse.builder()
                .fecha(fecha)
                .idServicio(serviceId)
                .duracionMinutos(servicio.getDuracion())
                .slots(slots)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRescheduleSlot(RescheduleSlotQuery query) {
        if (query.duracionReserva() <= 0 || query.duracionServicio() <= 0) {
            throw new ConflictException("La reserva no tiene una duración válida");
        }

        Instant slotInicio = query.fecha().atTime(query.horaInicio())
                .atZone(TimeConfig.ZONA_BOGOTA).toInstant();
        Instant slotFin = slotInicio.plus(query.duracionReserva(), ChronoUnit.MINUTES);

        if (slotInicio.isBefore(Instant.now(clock))) {
            throw new ConflictException("La nueva fecha y hora deben ser futuras");
        }

        int inicio = aMinutos(query.horaInicio());
        int fin = inicio + query.duracionReserva();

        List<HorarioFranja> schedule =
                scheduleLookupService.findServiceSchedules(query.serviceId());
        if (!isWithinSchedule(query.fecha(), inicio, fin, schedule)) {
            throw new ConflictException(
                    "El horario seleccionado no está dentro de la disponibilidad del servicio");
        }

        List<BloqueoFranja> blocks =
                scheduleLookupService.findProviderBlocks(query.providerId(), query.fecha());
        if (isBlocked(inicio, fin, blocks)) {
            throw new ConflictException(
                    "El horario seleccionado está bloqueado por el proveedor");
        }

        List<Reserva> reservas = loadOccupyingReservationsForValidation(
                query.serviceId(), query.fecha());
        if (isOccupied(inicio, fin, query.duracionServicio(), query.fecha(),
                reservas, query.excludeReservationId())) {
            throw new ConflictException("El horario seleccionado ya está ocupado");
        }

        log.debug("HU-17 franja valida servicio={} proveedor={} fecha={} hora={} duracion={} "
                        + "reservaExcluida={}",
                query.serviceId(), query.providerId(), query.fecha(), query.horaInicio(),
                query.duracionReserva(), query.excludeReservationId());
    }

    /**
     * HU-17: queries a wider window than the availability endpoint so a
     * reservation that starts before midnight and extends into the target day
     * is still considered. The Java interval check discards unrelated rows.
     */
    private List<Reserva> loadOccupyingReservationsForValidation(Long serviceId, LocalDate fecha) {
        Instant inicioVentana = inicioDelDiaBogota(fecha.minusDays(1));
        Instant finVentana = inicioDelDiaBogota(fecha.plusDays(2));

        return reservaRepository.findOccupyingReservationsInRange(
                serviceId, EstadoReserva.occupyingStateIds(), inicioVentana, finVentana);
    }

    private boolean isWithinSchedule(LocalDate fecha, int inicio, int fin,
                                     List<HorarioFranja> schedule) {
        int diaSemana = fecha.getDayOfWeek().getValue();

        for (HorarioFranja franja : schedule) {
            if (franja.diaSemana() != diaSemana) {
                continue;
            }
            int inicioFranja = aMinutos(franja.horaInicio());
            int finFranja = aMinutos(franja.horaFin());
            if (finFranja <= inicioFranja) {
                finFranja = finFranja + MINUTOS_POR_DIA;
            }
            if (inicio >= inicioFranja && fin <= finFranja) {
                return true;
            }
        }
        return false;
    }

    private List<Reserva> loadOccupyingReservations(Long serviceId, LocalDate fecha) {
        // La fecha del endpoint es local (America/Bogota). La medianoche local
        // se convierte a Instant (UTC) antes de consultar Reserva.horaInicio,
        // que es un timestamptz. Ej.: 2026-11-19 00:00 Bogota = 2026-11-19T05:00Z.
        Instant inicioDia = inicioDelDiaBogota(fecha);
        Instant inicioSiguiente = inicioDelDiaBogota(fecha.plusDays(1));

        List<Reserva> reservas = reservaRepository.findOccupyingReservationsInRange(
                serviceId, EstadoReserva.occupyingStateIds(), inicioDia, inicioSiguiente);

        log.debug("Disponibilidad servicio={} fechaLocal={} rangoUTC=[{}, {}) reservasOcupantes={}",
                serviceId, fecha, inicioDia, inicioSiguiente, reservas.size());

        return reservas;
    }

    private Instant inicioDelDiaBogota(LocalDate fecha) {
        return fecha.atStartOfDay(TimeConfig.ZONA_BOGOTA).toInstant();
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
        if (isOccupied(inicio, fin, duracion, fecha, reservas, null)) {
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

    private boolean isOccupied(int inicio, int fin, int duracion, LocalDate fecha,
                               List<Reserva> reservas, Long excludeReservationId) {
        // Se comparan intervalos absolutos (Instant). El slot se construye
        // como hora local de Bogota y se convierte a UTC; la reserva ya esta
        // en UTC. Asi una reserva a las 22:00 Bogota (03:00Z del dia
        // siguiente) se cruza correctamente con su slot.
        Instant slotInicio = aInstantBogota(fecha, inicio);
        Instant slotFin = aInstantBogota(fecha, fin);

        for (Reserva reserva : reservas) {
            if (reserva.getHoraInicio() == null || reserva.getIdEstado() == null) {
                continue;
            }
            if (excludeReservationId != null
                    && excludeReservationId.equals(reserva.getId())) {
                continue;
            }
            if (!EstadoReserva.fromId(reserva.getIdEstado()).occupiesSchedule()) {
                continue;
            }

            Instant reservaInicio = reserva.getHoraInicio();
            Instant reservaFin = reservaInicio.plus(
                    resolveDuracion(reserva, duracion), ChronoUnit.MINUTES);

            if (reservaInicio.isBefore(slotFin) && reservaFin.isAfter(slotInicio)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convierte un minuto local de Bogota (puede superar 1440 cuando la franja
     * cruza medianoche) al Instant UTC equivalente.
     */
    private Instant aInstantBogota(LocalDate fecha, int minutosDelDia) {
        int diasExtra = Math.floorDiv(minutosDelDia, MINUTOS_POR_DIA);
        int minutosNormalizados = Math.floorMod(minutosDelDia, MINUTOS_POR_DIA);

        return fecha.plusDays(diasExtra)
                .atTime(LocalTime.ofSecondOfDay(minutosNormalizados * 60L))
                .atZone(TimeConfig.ZONA_BOGOTA)
                .toInstant();
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
