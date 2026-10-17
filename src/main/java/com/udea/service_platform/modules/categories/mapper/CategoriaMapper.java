package com.udea.service_platform.modules.categories.mapper;

import com.udea.service_platform.modules.categories.dto.CategoriaRequest;
import com.udea.service_platform.modules.categories.dto.CategoriaResponse;
import com.udea.service_platform.modules.categories.model.Categoria;

/**
 * Single Responsibility: owns all Categoria <-> DTO transformations.
 */
public final class CategoriaMapper {

    private CategoriaMapper() {}

    public static Categoria toEntity(CategoriaRequest request) {
        return Categoria.builder()
                .nombre(request.getNombre().trim())
                .descripcion(request.getDescripcion() == null ? null : request.getDescripcion().trim())
                .build();
    }

    public static CategoriaResponse toResponse(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .build();
    }
}
