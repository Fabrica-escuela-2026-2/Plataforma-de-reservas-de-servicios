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
        long idCategoria = asegurarCategoria(jdbcTemplate, categoria);
        jdbcTemplate.update("INSERT INTO \"Servicios\" "
                        + "(id_servicio, nombre, categoria_servicios, duracion, precio, "
                        + "usuario_proveedor, id_recursos, activo) "
                        + "VALUES (?, ?, ?, 30, 1000, ?, 1, ?)",
                id, "Servicio " + id, idCategoria, idProveedor, activo);
    }

    /**
     * Returns the category id, creating the row when needed. The services table
     * now references "Categorias" through categoria_servicios.
     */
    public static long asegurarCategoria(JdbcTemplate jdbcTemplate, String nombre) {
        jdbcTemplate.update(
                "INSERT INTO \"Categorias\" (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING",
                nombre);
        return jdbcTemplate.queryForObject(
                "SELECT id_categoria FROM \"Categorias\" WHERE nombre = ?",
                Long.class, nombre);
    }
}
