package com.udea.service_platform.modules.users.mapper;

import com.udea.service_platform.modules.users.dto.ClientDetailResponse;
import com.udea.service_platform.modules.users.dto.ClientSummaryResponse;
import com.udea.service_platform.modules.users.dto.RoleResponse;
import com.udea.service_platform.modules.users.dto.UserRequest;
import com.udea.service_platform.modules.users.dto.UserResponse;
import com.udea.service_platform.modules.users.dto.UserProfileResponse;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;

/**
 * Single Responsibility: owns all User <-> DTO transformations.
 * Services orchestrate; this class formats.
 */
public final class UserMapper {

    private UserMapper() {}

    public static User toEntity(UserRequest request, Role role, String encodedPassword) {
        return User.builder()
                .nombre(request.getNombre())
                .correo(request.getCorreo())
                .password(encodedPassword)
                .apellido(request.getApellido() != null ? request.getApellido() : "")
                .idTipoDocumento(request.getIdTipoDocumento() != null ? request.getIdTipoDocumento() : 1L)
                .numeroDocumento(request.getNumeroDocumento() != null ? request.getNumeroDocumento() : "00000000")
                .telefono(request.getTelefono() != null ? request.getTelefono() : "0000000000")
                .idCiudad(request.getIdCiudad() != null ? request.getIdCiudad() : 1L)
                .idTipoProveedor(request.getIdTipoProveedor())
                .role(role)
                .build();
    }

    public static UserResponse toUserResponse(User user) {
        RoleResponse roleResponse = RoleResponse.builder()
                .idRol(user.getRole().getIdRol())
                .nombre(user.getRole().getNombre())
                .build();

        return UserResponse.builder()
                .idUsuario(user.getIdUsuario())
                .nombre(user.getNombre())
                .correo(user.getCorreo())
                .password(user.getPassword())
                .apellido(user.getApellido())
                .idTipoDocumento(user.getIdTipoDocumento())
                .numeroDocumento(user.getNumeroDocumento())
                .telefono(user.getTelefono())
                .idCiudad(user.getIdCiudad())
                .idTipoProveedor(user.getIdTipoProveedor())
                .role(roleResponse)
                .build();
    }

    public static UserProfileResponse toUserProfile(User user) {
        RoleResponse roleResponse = RoleResponse.builder()
                .idRol(user.getRole().getIdRol())
                .nombre(user.getRole().getNombre())
                .build();

        return UserProfileResponse.builder()
                .idUsuario(user.getIdUsuario())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .correo(user.getCorreo())
                .telefono(user.getTelefono())
                .idCiudad(user.getIdCiudad())
                .idTipoDocumento(user.getIdTipoDocumento())
                .numeroDocumento(user.getNumeroDocumento())
                .idTipoProveedor(user.getIdTipoProveedor())
                .notasEspeciales(user.getNotasEspeciales())
                .estadoCuenta(user.getEstadoCuenta())
                .role(roleResponse)
                .build();
    }

    public static ClientSummaryResponse toClientSummary(User user) {
        return ClientSummaryResponse.builder()
                .idUsuario(user.getIdUsuario())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .correo(user.getCorreo())
                .telefono(user.getTelefono())
                .estadoCuenta(user.getEstadoCuenta())
                .build();
    }

    public static ClientDetailResponse toClientDetail(User user) {
        return ClientDetailResponse.builder()
                .idUsuario(user.getIdUsuario())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .correo(user.getCorreo())
                .telefono(user.getTelefono())
                .numeroDocumento(user.getNumeroDocumento())
                .idTipoDocumento(user.getIdTipoDocumento())
                .idCiudad(user.getIdCiudad())
                .estadoCuenta(user.getEstadoCuenta())
                .notasEspeciales(user.getNotasEspeciales())
                .build();
    }
}
