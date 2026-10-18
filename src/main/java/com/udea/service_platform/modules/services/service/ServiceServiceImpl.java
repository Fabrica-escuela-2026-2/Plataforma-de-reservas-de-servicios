package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.dto.ServiceUpdateRequest;
import com.udea.service_platform.modules.services.mapper.ServiceMapper;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import com.udea.service_platform.modules.services.repository.ServiceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

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
    public ServiceResponse inactivateService(Long serviceId, Long idProveedor) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("El servicio no existe"));

        if (!service.getIdProveedor().equals(idProveedor)) {
            throw new AccessDeniedException("El servicio no pertenece al proveedor");
        }

        if (Boolean.TRUE.equals(service.getActivo())) {
            service.setActivo(false);
            service = serviceRepository.save(service);
        }

        return ServiceMapper.toResponse(service);
    }

    @Override
    public Page<ServiceResponse> getPublicCatalog(Long providerId, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        var spec = ServiceSpecification.buildFilter(providerId, category, minPrice, maxPrice);
        return serviceRepository.findAll(spec, pageable)
                .map(ServiceMapper::toResponse);
    }

    @Override
    @Transactional
    public ServiceResponse updateService(Long id, ServiceUpdateRequest request, Long idProveedor) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe"));

        if (!Objects.equals(service.getIdProveedor(), idProveedor)) {
            // En main el handler genérico convertiría AccessDeniedException a 500.
            // Queda como pendiente migrar a 403 cuando se mergee el PR de seguridad.
            throw new IllegalArgumentException("No tiene permisos para modificar este servicio");
        }

        if (!recursoRepository.existsById(request.getIdRecursos())) {
            throw new IllegalArgumentException("El recurso seleccionado no existe");
        }

        service.setNombre(request.getNombre());
        service.setDescripcion(request.getDescripcion());
        service.setCategoria(request.getCategoria());
        service.setDuracion(request.getDuracion());
        service.setPrecio(request.getPrecio());
        service.setIdRecursos(request.getIdRecursos());
        // No se modifica id, idProveedor, ni activo

        Service savedService = serviceRepository.save(service);
        return ServiceMapper.toResponse(savedService);
    }
}
