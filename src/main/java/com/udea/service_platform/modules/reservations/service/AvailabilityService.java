package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.RescheduleSlotQuery;

import java.time.LocalDate;

/**
 * Dependency Inversion: the availability controller depends on this
 * abstraction, not on the concrete implementation.
 */
public interface AvailabilityService {

    AvailabilityResponse getAvailability(Long serviceId, LocalDate fecha);

    /**
     * HU-17: validates that a reschedule target is in the future, inside the
     * provider's weekly schedule, not blocked and not occupied by another
     * reservation (the reservation being rescheduled is excluded).
     */
    void validateRescheduleSlot(RescheduleSlotQuery query);
}
