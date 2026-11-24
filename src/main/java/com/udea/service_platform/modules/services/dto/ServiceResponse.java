package com.udea.service_platform.modules.services.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String categoria;
    private Integer duracion;
    private BigDecimal precio;
    private Long idProveedor;
    private Long idRecursos;
    private Integer aforo;
    private Boolean activo;
}
