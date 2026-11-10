package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceLookupServiceImpl implements ServiceLookupService {

    private final ServiceRepository serviceRepository;

    @Override
    public Service findActiveService(Long serviceId) {
        Service servicio = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio no existe"));

        if (!servicio.getActivo()) {
            throw new IllegalArgumentException("El servicio no está activo");
        }

        return servicio;
    }
}
