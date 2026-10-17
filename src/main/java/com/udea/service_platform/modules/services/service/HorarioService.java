package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.HorarioRequest;
import com.udea.service_platform.modules.services.dto.HorarioResponse;

import java.util.List;

/**
 * Dependency Inversion: controllers depend on this abstraction,
 * not on the concrete implementation.
 */
public interface HorarioService {

    List<HorarioResponse> defineSchedule(Long serviceId, List<HorarioRequest> requests, Long providerId);

    List<HorarioResponse> getSchedule(Long serviceId, Long providerId);
}
