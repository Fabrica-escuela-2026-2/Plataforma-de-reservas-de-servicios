package com.udea.service_platform.modules.users.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private Long idCiudad;
    private Long idTipoDocumento;
    private String numeroDocumento;
    private Long idTipoProveedor;
    private String notasEspeciales;
    private String estadoCuenta;
    private RoleResponse role;
}
