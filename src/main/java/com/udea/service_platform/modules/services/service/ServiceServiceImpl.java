package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.mapper.ServiceMapper;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import com.udea.service_platform.modules.services.repository.ServiceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final RecursoRepository recursoRepository;

    @Override
    public ServiceResponse createService(ServiceRequest request, Long idProveedor) {
        if (!recursoRepository.existsById(request.getIdRecursos())) {
            throw new IllegalArgumentException("El recurso seleccionado no existe");
        }

        Service service = ServiceMapper.toEntity(request, idProveedor);

        Service savedService = serviceRepository.save(service);

        return ServiceMapper.toResponse(savedService);
    }

    @Override
    public Page<ServiceResponse> getPublicCatalog(Long providerId, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        var spec = ServiceSpecification.buildFilter(providerId, category, minPrice, maxPrice);
        return serviceRepository.findAll(spec, pageable)
                .map(ServiceMapper::toResponse);
    }
}
