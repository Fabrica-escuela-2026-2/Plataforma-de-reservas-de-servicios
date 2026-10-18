package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Dependency Inversion: controllers depend on this abstraction,
 * not on the concrete implementation.
 */
public interface ServiceService {

    ServiceResponse createService(ServiceRequest request, Long idProveedor);

    ServiceResponse inactivateService(Long serviceId, Long idProveedor);

    Page<ServiceResponse> getPublicCatalog(Long providerId, String category,
                                           BigDecimal minPrice, BigDecimal maxPrice,
                                           Pageable pageable);

    ServiceResponse updateService(Long id, ServiceRequest request, Long idProveedor);
}
