package com.udea.service_platform.modules.users.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;

    @Size(max = 255, message = "El apellido no puede superar 255 caracteres")
    private String apellido;

    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;

    private Long idCiudad;

    private Long idTipoDocumento;

    @Size(max = 255, message = "El número de documento no puede superar 255 caracteres")
    private String numeroDocumento;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String notasEspeciales;
}
