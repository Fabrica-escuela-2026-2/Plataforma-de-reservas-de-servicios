package com.udea.service_platform.modules.reservations.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaResponse {

    private Long id;
    private String servicioNombre;
    private String clienteNombre;
    private String clienteCorreo;
    private LocalTime fechaInicio;
    private LocalDate fecha;
    private String estado;
    private BigDecimal valorAcordado;
    private Integer duracionAcordada;
}
