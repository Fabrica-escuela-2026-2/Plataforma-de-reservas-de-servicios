package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.BloqueoRequest;
import com.udea.service_platform.modules.services.dto.BloqueoResponse;

import java.util.List;

/**
 * Dependency Inversion: controllers depend on this abstraction,
 * not on the concrete implementation.
 */
public interface BloqueoService {

    BloqueoResponse createBlock(BloqueoRequest request, Long providerId);

    List<BloqueoResponse> getActiveBlocks(Long providerId);

    void deleteBlock(Long blockId, Long providerId);
}
