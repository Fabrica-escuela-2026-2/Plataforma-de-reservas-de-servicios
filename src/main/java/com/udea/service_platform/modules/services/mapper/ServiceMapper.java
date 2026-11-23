package com.udea.service_platform.modules.services.mapper;

import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.model.Service;

/**
 * Single Responsibility: owns all Service <-> DTO transformations.
 * Services orchestrate; this class formats.
 */
public final class ServiceMapper {

    private ServiceMapper() {}

    public static Service toEntity(ServiceRequest request, Long idProveedor) {
        return Service.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .duracion(request.getDuracion())
                .precio(request.getPrecio())
                .idProveedor(idProveedor)
                .idRecursos(request.getIdRecursos())
                .activo(true)
                .build();
    }

    public static ServiceResponse toResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .nombre(service.getNombre())
                .descripcion(service.getDescripcion())
                .categoria(service.getCategoria())
                .duracion(service.getDuracion())
                .precio(service.getPrecio())
                .idProveedor(service.getIdProveedor())
                .idRecursos(service.getIdRecursos())
                .aforo(service.getAforo())
                .activo(service.getActivo())
                .build();
    }
}
