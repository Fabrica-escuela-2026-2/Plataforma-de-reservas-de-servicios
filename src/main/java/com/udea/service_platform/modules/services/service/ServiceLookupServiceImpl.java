package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceLookupServiceImpl implements ServiceLookupService {

    private static final String SERVICIO_NO_DISPONIBLE = "El servicio no está disponible";

    private final ServiceRepository serviceRepository;

    @Override
    public Service findActiveService(Long serviceId) {
        Service servicio = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe"));

        if (!servicio.getActivo()) {
            throw new IllegalArgumentException("El servicio no está activo");
        }

        return servicio;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Service lockActiveService(Long serviceId) {
        Service servicio = serviceRepository.findByIdForUpdate(serviceId)
                .orElseThrow(() -> new IllegalArgumentException(SERVICIO_NO_DISPONIBLE));

        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new IllegalArgumentException(SERVICIO_NO_DISPONIBLE);
        }

        return servicio;
    }
}
