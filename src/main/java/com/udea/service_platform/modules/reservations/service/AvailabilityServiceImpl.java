package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.AvailabilitySlotResponse;
import com.udea.service_platform.modules.reservations.dto.RescheduleSlotQuery;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.model.Service;
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
    private final ReservationSlotPolicy slotPolicy;
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

        SlotContext context = slotPolicy.loadForAvailability(
                serviceId,
                servicio.getIdRecursos(),
                servicio.getAforo(),
                servicio.getIdProveedor(),
                fecha,
                servicio.getDuracion());

        List<AvailabilitySlotResponse> slots = buildSlots(fecha, servicio, context);

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
        if (slotInicio.isBefore(Instant.now(clock))) {
            throw new ConflictException("La nueva fecha y hora deben ser futuras");
        }

        SlotContext context = slotPolicy.loadForMutation(
                query.serviceId(),
                query.idRecurso(),
                query.aforo(),
                query.providerId(),
                query.fecha(),
                query.duracionServicio(),
                query.excludeReservationId());
        SlotDecision decision = slotPolicy.evaluate(context, query.horaInicio(), query.duracionReserva());

        if (decision.reason() != SlotReason.OK) {
            throw new ConflictException(mensajeReagendamiento(decision.reason(), decision.mensaje()));
        }

        log.debug("HU-17 franja valida servicio={} proveedor={} fecha={} hora={} duracion={} reservaExcluida={}",
                query.serviceId(), query.providerId(), query.fecha(), query.horaInicio(),
                query.duracionReserva(), query.excludeReservationId());
    }

    private String mensajeReagendamiento(SlotReason reason, String mensajePolitica) {
        return switch (reason) {
            case OUT_OF_SCHEDULE ->
                    "El horario seleccionado no está dentro de la disponibilidad del servicio";
            case PROVIDER_BLOCK -> "El horario seleccionado está bloqueado por el proveedor";
            case RESOURCE_BUSY, CAPACITY_FULL -> mensajePolitica;
            default -> throw new IllegalStateException("Franja ya validada: " + reason);
        };
    }

    private List<AvailabilitySlotResponse> buildSlots(LocalDate fecha, Service servicio, SlotContext context) {
        int duracion = servicio.getDuracion();
        boolean hoy = fecha.equals(LocalDate.now(clock));
        int minutoActual = LocalTime.now(clock).toSecondOfDay() / 60;
        int diaSemana = fecha.getDayOfWeek().getValue();

        List<AvailabilitySlotResponse> slots = new ArrayList<>();
        for (HorarioFranja franja : context.schedule()) {
            if (franja.diaSemana() != diaSemana) {
                continue;
            }
            appendFranjaSlots(slots, franja, duracion, hoy, minutoActual, fecha, context);
        }
        return slots;
    }

    private void appendFranjaSlots(List<AvailabilitySlotResponse> slots, HorarioFranja franja,
                                   int duracion, boolean hoy, int minutoActual, LocalDate fecha,
                                   SlotContext context) {
        int inicioFranja = aMinutos(franja.horaInicio());
        int finFranja = aMinutos(franja.horaFin());
        if (finFranja <= inicioFranja) {
            finFranja += MINUTOS_POR_DIA;
        }

        int cursor = inicioFranja;
        while (cursor + duracion <= finFranja) {
            int inicioSlot = cursor;
            int finSlot = cursor + duracion;
            cursor = finSlot;

            if (hoy && inicioSlot < minutoActual) {
                continue;
            }

            SlotDecision decision = slotPolicy.evaluate(
                    context, aHoraLocal(inicioSlot), duracion);
            slots.add(AvailabilitySlotResponse.builder()
                    .horaInicio(aHoraLocal(inicioSlot))
                    .horaFin(aHoraLocal(finSlot))
                    .estado(decision.estado())
                    .cuposTotales(decision.cuposTotales())
                    .cuposDisponibles(decision.cuposDisponibles())
                    .mensaje(decision.mensaje())
                    .build());
        }
    }

    private int aMinutos(LocalTime hora) {
        return hora.toSecondOfDay() / 60;
    }

    private LocalTime aHoraLocal(int minutos) {
        return LocalTime.ofSecondOfDay((minutos % MINUTOS_POR_DIA) * 60L);
    }
}
