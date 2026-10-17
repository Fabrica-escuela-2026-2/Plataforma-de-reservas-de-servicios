package com.udea.service_platform.modules.categories.service;

import com.udea.service_platform.modules.categories.dto.CategoriaRequest;
import com.udea.service_platform.modules.categories.dto.CategoriaResponse;
import com.udea.service_platform.modules.categories.mapper.CategoriaMapper;
import com.udea.service_platform.modules.categories.model.Categoria;
import com.udea.service_platform.modules.categories.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Override
    public CategoriaResponse createCategory(CategoriaRequest request) {
        String nombre = request.getNombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("La categoría ya existe");
        }

        Categoria saved = categoriaRepository.save(CategoriaMapper.toEntity(request));

        return CategoriaMapper.toResponse(saved);
    }

    @Override
    public List<CategoriaResponse> getAllCategories() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(CategoriaMapper::toResponse)
                .toList();
    }
}
