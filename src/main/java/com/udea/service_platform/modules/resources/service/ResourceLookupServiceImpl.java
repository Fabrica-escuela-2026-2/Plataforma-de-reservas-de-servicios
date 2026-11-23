package com.udea.service_platform.modules.resources.service;

import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResourceLookupServiceImpl implements ResourceLookupService {

    private final RecursoRepository recursoRepository;

    @Override
    @Transactional(readOnly = true)
    public void validateLinkable(Long idRecurso, Long idProveedor) {
        Recurso recurso = recursoRepository.findById(idRecurso)
                .orElseThrow(() -> new IllegalArgumentException("El recurso seleccionado no existe"));
        if (!Boolean.TRUE.equals(recurso.getActivo())) {
            throw new IllegalArgumentException("El recurso seleccionado no está activo");
        }
        if (!Objects.equals(recurso.getIdProveedor(), idProveedor)) {
            throw new AccessDeniedException("El recurso no pertenece al proveedor");
        }
    }
}
