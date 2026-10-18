package com.udea.service_platform.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderOnboardingRequest {

    @NotNull(message = "Debe seleccionar un tipo de proveedor")
    private Long idTipoProveedor;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String notasEspeciales;
}
