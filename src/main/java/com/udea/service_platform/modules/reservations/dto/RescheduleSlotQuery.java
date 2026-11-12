package com.udea.service_platform.modules.reservations.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * HU-17: input for validating a reschedule target slot against the provider's
 * schedule, blocks and existing reservations.
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
        Long excludeReservationId) {
}
