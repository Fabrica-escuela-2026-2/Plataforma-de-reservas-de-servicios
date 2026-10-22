package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade published by the services module for schedule-driven features.
 * Availability (and any other module) resolves weekly schedules and provider
 * blocks through this contract instead of injecting HorarioRepository or
 * BloqueoRepository directly.
 */
public interface ScheduleLookupService {

    /**
     * Weekly schedule of a service, as light DTOs (no JPA entities).
     */
    List<HorarioFranja> findServiceSchedules(Long serviceId);

    /**
     * Provider blocks for a given date. Empty hours mean a full-day block.
     */
    List<BloqueoFranja> findProviderBlocks(Long providerId, LocalDate fecha);
}
