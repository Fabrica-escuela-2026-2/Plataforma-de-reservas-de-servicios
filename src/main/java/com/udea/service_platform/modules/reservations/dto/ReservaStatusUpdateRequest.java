package com.udea.service_platform.modules.reservations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaStatusUpdateRequest {

    @NotBlank(message = "El estado es obligatorio")
    @Schema(
            description = "Nuevo estado operativo de la reserva",
            example = "CONFIRMADA",
            allowableValues = {"CONFIRMADA", "EN_PROCESO", "COMPLETADA", "NO_ASISTIO"})
    private String estado;
}
