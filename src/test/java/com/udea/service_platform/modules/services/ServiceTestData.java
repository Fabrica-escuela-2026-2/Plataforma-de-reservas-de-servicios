package com.udea.service_platform.modules.services;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Native SQL fixtures shared by the services integration tests.
 * Explicit high ids keep shared sequences untouched.
 */
public final class ServiceTestData {

    private ServiceTestData() {
    }

    public static void insertarProveedor(JdbcTemplate jdbcTemplate, long id, String correo) {
        jdbcTemplate.update("INSERT INTO \"Usuarios\" "
                        + "(id_usuario, nombre, correo, password_hash, estado_cuenta, rol_usuario) "
                        + "VALUES (?, ?, ?, 'hash', 'ACTIVA', "
                        + "(SELECT id_rol FROM \"Roles\" WHERE nombre = 'Proveedor de Servicios' LIMIT 1))",
                id, "Proveedor " + id, correo);
    }

    public static void insertarServicio(JdbcTemplate jdbcTemplate, long id, long idProveedor,
                                        String categoria, boolean activo) {
        jdbcTemplate.update("INSERT INTO \"Servicios\" "
                        + "(id_servicio, nombre, categoria, duracion, precio, usuario_proveedor, id_recursos, activo) "
                        + "VALUES (?, ?, ?, 30, 1000, ?, 1, ?)",
                id, "Servicio " + id, categoria, idProveedor, activo);
    }
}
