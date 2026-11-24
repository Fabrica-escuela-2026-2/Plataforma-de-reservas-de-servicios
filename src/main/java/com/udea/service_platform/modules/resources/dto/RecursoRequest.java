package com.udea.service_platform.modules.resources.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecursoRequest {

    @NotBlank(message = "El nombre del recurso es obligatorio")
    private String nombre;

    private Long idProveedor;
}
