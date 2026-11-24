package com.udea.service_platform.modules.categories.service;

import com.udea.service_platform.modules.categories.model.Categoria;
import com.udea.service_platform.modules.categories.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaLookupServiceImpl implements CategoriaLookupService {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public CategoriaInfo resolveOrCreate(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }

        return categoriaRepository.findByNombreIgnoreCase(limpio)
                .map(this::toInfo)
                .orElseGet(() -> crearSiNoExiste(limpio));
    }

    private CategoriaInfo crearSiNoExiste(String nombre) {
        categoriaRepository.insertIfAbsent(nombre);
        return categoriaRepository.findByNombreIgnoreCase(nombre)
                .map(this::toInfo)
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo registrar la categoría " + nombre));
    }

    private CategoriaInfo toInfo(Categoria categoria) {
        return new CategoriaInfo(categoria.getId(), categoria.getNombre());
    }
}
