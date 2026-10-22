package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;

import java.time.LocalDate;

/**
 * Dependency Inversion: the availability controller depends on this
 * abstraction, not on the concrete implementation.
 */
public interface AvailabilityService {

    AvailabilityResponse getAvailability(Long serviceId, LocalDate fecha);
}
