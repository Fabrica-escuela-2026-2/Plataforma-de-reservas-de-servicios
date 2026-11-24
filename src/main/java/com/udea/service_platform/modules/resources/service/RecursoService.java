package com.udea.service_platform.modules.resources.service;

import com.udea.service_platform.modules.resources.dto.RecursoRequest;
import com.udea.service_platform.modules.resources.dto.RecursoResponse;

import java.util.List;

public interface RecursoService {

    RecursoResponse crear(RecursoRequest request, Long idProveedorToken, boolean esAdmin);

    List<RecursoResponse> misRecursos(Long idProveedor);

    List<RecursoResponse> listar(Long idProveedorFiltro);

    RecursoResponse inactivar(Long idRecurso, Long idProveedor, boolean esAdmin);
}
