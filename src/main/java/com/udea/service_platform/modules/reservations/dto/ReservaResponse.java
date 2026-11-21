package com.udea.service_platform.modules.reservations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(
            description = "Estado actual de la reserva",
            example = "ACTIVA",
            allowableValues = {
                    "ACTIVA", "CONFIRMADA", "EN_PROCESO",
                    "COMPLETADA", "CANCELADA", "NO_ASISTIO"
            })
    private String estado;
    private BigDecimal valorAcordado;
    private Integer duracionAcordada;
}
