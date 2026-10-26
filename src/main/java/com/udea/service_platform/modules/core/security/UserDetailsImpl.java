package com.udea.service_platform.modules.core.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long idUsuario;
    private final String correo;
    private final String password;
    private final String nombre;
    private final String rol;
    private final String estadoCuenta;

    public UserDetailsImpl(Long idUsuario, String correo, String password, String nombre, String rol) {
        this(idUsuario, correo, password, nombre, rol, com.udea.service_platform.modules.users.model.User.ESTADO_ACTIVA);
    }

    public UserDetailsImpl(Long idUsuario, String correo, String password, String nombre, String rol, String estadoCuenta) {
        this.idUsuario = idUsuario;
        this.correo = correo;
        this.password = password;
        this.nombre = nombre;
        this.rol = rol;
        this.estadoCuenta = estadoCuenta;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isEnabled() {
        return com.udea.service_platform.modules.users.model.User.ESTADO_ACTIVA.equals(estadoCuenta);
    }

    static String mapRole(String dbRoleName) {
        return switch (dbRoleName) {
            case "Cliente" -> "CLIENTE";
            case "Proveedor de Servicios" -> "PROVEEDOR";
            case "Administrador" -> "ADMIN";
            default -> dbRoleName.toUpperCase();
        };
    }
}
