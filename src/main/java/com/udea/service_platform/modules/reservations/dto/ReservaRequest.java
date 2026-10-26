package com.udea.service_platform.modules.reservations.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaRequest {

    @NotNull(message = "El ID del servicio es obligatorio")
    private Long idServicio;

    @NotNull(message = "La fecha es obligatoria")
    private String fecha;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private String fechaInicio;
}
