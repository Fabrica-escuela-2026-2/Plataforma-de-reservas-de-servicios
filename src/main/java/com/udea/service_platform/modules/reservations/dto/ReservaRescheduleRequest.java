package com.udea.service_platform.modules.reservations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaRescheduleRequest {

    @NotBlank(message = "La fecha es obligatoria")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)")
    @Schema(description = "Nueva fecha local (America/Bogota)", example = "2026-10-20")
    private String fecha;

    @NotBlank(message = "La fecha de inicio es obligatoria")
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "Formato de hora inválido. Use HH:mm (ej. 14:30)")
    @Schema(description = "Nueva hora local de inicio (America/Bogota)", example = "14:30")
    private String fechaInicio;
}
