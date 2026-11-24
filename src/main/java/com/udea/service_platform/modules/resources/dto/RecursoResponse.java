package com.udea.service_platform.modules.resources.dto;

import com.udea.service_platform.modules.resources.service.LinkedService;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecursoResponse {

    private Long id;
    private String nombre;
    private Long idProveedor;
    private Boolean activo;
    private List<LinkedService> servicios;
}
