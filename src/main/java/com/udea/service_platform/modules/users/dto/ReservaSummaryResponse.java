package com.udea.service_platform.modules.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaSummaryResponse {

    private Long id;
    private String servicioNombre;
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
}
