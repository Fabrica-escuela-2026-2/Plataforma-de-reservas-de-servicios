package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final RecursoRepository recursoRepository;

    public ServiceResponse createService(ServiceRequest request, Long idProveedor) {
        if (!recursoRepository.existsById(request.getIdRecursos())) {
            throw new IllegalArgumentException("El recurso seleccionado no existe");
        }

        Service service = Service.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .duracion(request.getDuracion())
                .precio(request.getPrecio())
                .idProveedor(idProveedor)
                .idRecursos(request.getIdRecursos())
                .build();

        Service savedService = serviceRepository.save(service);

        return ServiceResponse.builder()
                .id(savedService.getId())
                .nombre(savedService.getNombre())
                .descripcion(savedService.getDescripcion())
                .categoria(savedService.getCategoria())
                .duracion(savedService.getDuracion())
                .precio(savedService.getPrecio())
                .idProveedor(savedService.getIdProveedor())
                .idRecursos(savedService.getIdRecursos())
                .build();
    }
}
