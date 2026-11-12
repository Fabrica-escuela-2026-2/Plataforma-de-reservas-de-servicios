package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class ServiceAdministrationServiceImpl implements ServiceAdministrationService {

    private final ServiceRepository serviceRepository;

    @Override
    public int deactivateServicesByProvider(Long idProveedor) {
        return serviceRepository.deactivateServicesByProvider(idProveedor);
    }
}
