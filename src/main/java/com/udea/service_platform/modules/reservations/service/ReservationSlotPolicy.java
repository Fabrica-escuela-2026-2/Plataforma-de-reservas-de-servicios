package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.service.ScheduleLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * HU-20/HU-21: unica fuente de verdad para decidir si una franja esta libre.
 * Orden: fuera de horario o bloqueo del proveedor > recurso ocupado por otro
 * servicio > aforo agotado > libre. No adquiere locks: los flujos que escriben
 * toman advisory y FOR UPDATE antes de llamarla.
 */
@Component
@RequiredArgsConstructor
public class ReservationSlotPolicy {

    public static final String MSG_RECURSO_OCUPADO = "El recurso ya está ocupado en ese horario";
    public static final String MSG_SIN_CUPOS = "No hay cupos disponibles";

    private static final int MINUTOS_POR_DIA = 24 * 60;

    private final ScheduleLookupService scheduleLookupService;
    private final ReservaRepository reservaRepository;

    public SlotContext loadForAvailability(Long serviceId, Long idRecurso, Integer aforo,
                                           Long providerId, LocalDate fecha, int duracionServicio) {
        return load(serviceId, idRecurso, aforo, providerId, fecha, duracionServicio, false, null);
    }

    public SlotContext loadForMutation(Long serviceId, Long idRecurso, Integer aforo,
                                       Long providerId, LocalDate fecha, int duracionServicio,
                                       Long excludeReservationId) {
        return load(serviceId, idRecurso, aforo, providerId, fecha, duracionServicio, true, excludeReservationId);
    }

    private SlotContext load(Long serviceId, Long idRecurso, Integer aforo, Long providerId,
                             LocalDate fecha, int duracionServicio, boolean ventanaAmplia,
                             Long excludeReservationId) {
        List<HorarioFranja> schedule = scheduleLookupService.findServiceSchedules(serviceId);
        List<BloqueoFranja> blocks = scheduleLookupService.findProviderBlocks(providerId, fecha);

        LocalDate desde = ventanaAmplia ? fecha.minusDays(1) : fecha;
        LocalDate hasta = ventanaAmplia ? fecha.plusDays(2) : fecha.plusDays(1);
        Instant inicio = inicioDelDiaBogota(desde);
        Instant fin = inicioDelDiaBogota(hasta);

        List<Reserva> mismoServicio = reservaRepository.findOccupyingReservationsInRange(
                serviceId, EstadoReserva.occupyingStateIds(), inicio, fin);
        List<Reserva> otroServicio = idRecurso == null ? List.of()
                : reservaRepository.findOccupyingReservationsByRecursoInRange(
                        idRecurso, serviceId, EstadoReserva.occupyingStateIds(), inicio, fin);

        return new SlotContext(serviceId, idRecurso, aforo, providerId, fecha, duracionServicio,
                schedule, blocks, mismoServicio, otroServicio, excludeReservationId);
    }

    public SlotDecision evaluate(SlotContext context, LocalTime horaInicioSlot, Integer duracionReserva) {
        int cuposTotales = normalizarAforo(context.aforo());
        int duracion = duracionReserva != null && duracionReserva > 0
                ? duracionReserva : context.duracionServicio();

        int inicio = aMinutos(horaInicioSlot);
        int fin = inicio + Math.max(duracion, 0);

        if (!estaDentroDelHorario(context.fecha(), inicio, fin, context.schedule())) {
            return new SlotDecision(EstadoSlot.BLOQUEADA, SlotReason.OUT_OF_SCHEDULE, cuposTotales, 0, null);
        }
        if (estaBloqueado(inicio, fin, context.blocks())) {
            return new SlotDecision(EstadoSlot.BLOQUEADA, SlotReason.PROVIDER_BLOCK, cuposTotales, 0, null);
        }

        Instant inicioAbsoluto = aInstantBogota(context.fecha(), inicio);
        Instant finAbsoluto = aInstantBogota(context.fecha(), fin);

        if (context.idRecurso() != null && haySolape(context.otherServiceReservations(), inicioAbsoluto,
                finAbsoluto, context.duracionServicio(), context.excludeReservationId())) {
            return new SlotDecision(EstadoSlot.OCUPADA, SlotReason.RESOURCE_BUSY,
                    cuposTotales, 0, MSG_RECURSO_OCUPADO);
        }

        int ocupados = contarSolapes(context.sameServiceReservations(), inicioAbsoluto, finAbsoluto,
                context.duracionServicio(), context.excludeReservationId());
        int disponibles = Math.max(0, cuposTotales - ocupados);
        if (disponibles == 0) {
            return new SlotDecision(EstadoSlot.OCUPADA, SlotReason.CAPACITY_FULL,
                    cuposTotales, 0, MSG_SIN_CUPOS);
        }
        return new SlotDecision(EstadoSlot.LIBRE, SlotReason.OK, cuposTotales, disponibles, null);
    }

    public int peakConcurrentOccupyingReservations(Long serviceId, Integer duracionServicio, Instant desde) {
        List<Reserva> reservas = reservaRepository.findFutureOccupyingReservations(
                serviceId, EstadoReserva.occupyingStateIds(), desde);

        List<Evento> eventos = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getHoraInicio() == null || reserva.getIdEstado() == null
                    || !EstadoReserva.fromId(reserva.getIdEstado()).occupiesSchedule()) {
                continue;
            }
            Integer duracion = reserva.getDuracionAcordada() != null
                    ? reserva.getDuracionAcordada() : duracionServicio;
            Instant fin = reserva.getHoraInicio().plus(
                    Math.max(duracion == null ? 0 : duracion, 0), ChronoUnit.MINUTES);
            eventos.add(new Evento(reserva.getHoraInicio(), 1));
            eventos.add(new Evento(fin, -1));
        }

        eventos.sort(Comparator.comparing(Evento::instant).thenComparingInt(Evento::delta));
        int actual = 0;
        int pico = 0;
        for (Evento evento : eventos) {
            actual += evento.delta();
            pico = Math.max(pico, actual);
        }
        return Math.max(pico, 0);
    }

    public int normalizarAforo(Integer aforo) {
        return aforo == null || aforo < 1 ? 1 : aforo;
    }

    private boolean estaDentroDelHorario(LocalDate fecha, int inicio, int fin,
                                         List<HorarioFranja> schedule) {
        int diaSemana = fecha.getDayOfWeek().getValue();
        for (HorarioFranja franja : schedule) {
            if (franja.diaSemana() != diaSemana) {
                continue;
            }
            int inicioFranja = aMinutos(franja.horaInicio());
            int finFranja = aMinutos(franja.horaFin());
            if (finFranja <= inicioFranja) {
                finFranja += MINUTOS_POR_DIA;
            }
            if (inicio >= inicioFranja && fin <= finFranja) {
                return true;
            }
        }
        return false;
    }

    private boolean estaBloqueado(int inicio, int fin, List<BloqueoFranja> blocks) {
        for (BloqueoFranja bloqueo : blocks) {
            if (bloqueo.horaInicio() == null || bloqueo.horaFin() == null) {
                return true;
            }
            int inicioBloqueo = aMinutos(bloqueo.horaInicio());
            int finBloqueo = aMinutos(bloqueo.horaFin());
            if (finBloqueo <= inicioBloqueo) {
                finBloqueo += MINUTOS_POR_DIA;
            }
            if (inicioBloqueo < fin && finBloqueo > inicio) {
                return true;
            }
        }
        return false;
    }

    private int contarSolapes(List<Reserva> reservas, Instant inicio, Instant fin,
                              int duracionServicio, Long excludeReservationId) {
        int total = 0;
        for (Reserva reserva : reservas) {
            if (solapa(reserva, inicio, fin, duracionServicio, excludeReservationId)) {
                total++;
            }
        }
        return total;
    }

    private boolean haySolape(List<Reserva> reservas, Instant inicio, Instant fin,
                              int duracionServicio, Long excludeReservationId) {
        for (Reserva reserva : reservas) {
            if (solapa(reserva, inicio, fin, duracionServicio, excludeReservationId)) {
                return true;
            }
        }
        return false;
    }

    private boolean solapa(Reserva reserva, Instant inicioSlot, Instant finSlot,
                           int duracionServicio, Long excludeReservationId) {
        if (reserva.getHoraInicio() == null || reserva.getIdEstado() == null) {
            return false;
        }
        if (excludeReservationId != null && excludeReservationId.equals(reserva.getId())) {
            return false;
        }
        if (!EstadoReserva.fromId(reserva.getIdEstado()).occupiesSchedule()) {
            return false;
        }
        int duracion = reserva.getDuracionAcordada() != null
                ? reserva.getDuracionAcordada() : duracionServicio;
        Instant reservaInicio = reserva.getHoraInicio();
        Instant reservaFin = reservaInicio.plus(Math.max(duracion, 0), ChronoUnit.MINUTES);
        return reservaInicio.isBefore(finSlot) && reservaFin.isAfter(inicioSlot);
    }

    private Instant inicioDelDiaBogota(LocalDate fecha) {
        return fecha.atStartOfDay(TimeConfig.ZONA_BOGOTA).toInstant();
    }

    private Instant aInstantBogota(LocalDate fecha, int minutosDelDia) {
        int diasExtra = Math.floorDiv(minutosDelDia, MINUTOS_POR_DIA);
        int minutosNormalizados = Math.floorMod(minutosDelDia, MINUTOS_POR_DIA);
        return fecha.plusDays(diasExtra)
                .atTime(LocalTime.ofSecondOfDay(minutosNormalizados * 60L))
                .atZone(TimeConfig.ZONA_BOGOTA)
                .toInstant();
    }

    private int aMinutos(LocalTime hora) {
        return hora.toSecondOfDay() / 60;
    }

    private record Evento(Instant instant, int delta) {
    }
}
