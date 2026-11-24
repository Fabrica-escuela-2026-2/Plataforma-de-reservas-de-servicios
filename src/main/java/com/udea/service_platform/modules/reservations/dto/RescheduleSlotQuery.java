package com.udea.service_platform.modules.reservations.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * HU-17/HU-20/HU-21: input for validating a reschedule target slot against the
 * provider's schedule, blocks, shared resource and reservation capacity.
 *
 * <p>Two durations are needed: {@code duracionReserva} sizes the target
 * interval, while {@code duracionServicio} is the fallback used for other
 * reservations whose {@code duracionAcordada} is null.</p>
 */
public record RescheduleSlotQuery(
        Long serviceId,
        Long providerId,
        LocalDate fecha,
        LocalTime horaInicio,
        int duracionReserva,
        int duracionServicio,
        Long excludeReservationId,
        Long idRecurso,
        Integer aforo) {

    public RescheduleSlotQuery(Long serviceId, Long providerId, LocalDate fecha, LocalTime horaInicio,
                               int duracionReserva, int duracionServicio, Long excludeReservationId) {
        this(serviceId, providerId, fecha, horaInicio, duracionReserva, duracionServicio,
                excludeReservationId, null, null);
    }
}
