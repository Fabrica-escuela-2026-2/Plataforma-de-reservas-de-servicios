package com.udea.service_platform.modules.users.service;

/**
 * Published contract of the users module for other modules.
 * Contact data of any user (client or provider), without leaking the
 * User entity or UserRepository across module boundaries.
 * {@code telefono} may be null: the phone is optional in the profile.
 */
public record ContactInfo(
        Long idUsuario,
        String nombreCompleto,
        String correo,
        String telefono
) {}
