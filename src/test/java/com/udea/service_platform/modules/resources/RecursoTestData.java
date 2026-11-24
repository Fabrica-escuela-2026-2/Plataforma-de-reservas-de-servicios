package com.udea.service_platform.modules.resources;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

/**
 * Native SQL fixtures shared by the resources integration tests.
 * Explicit high ids keep shared sequences untouched.
 */
public final class RecursoTestData {

    public static final long PROVEEDOR = 9701L;
    public static final long OTRO_PROVEEDOR = 9702L;
    public static final long RECURSO = 9711L;
    public static final long OTRO_RECURSO = 9712L;
    public static final long SERVICIO = 9801L;
    public static final long RESERVA = 9811L;

    private RecursoTestData() {
    }

    public static void insertarUsuario(JdbcTemplate jdbcTemplate, long id, String rol) {
        jdbcTemplate.update("INSERT INTO \"Usuarios\" "
                        + "(id_usuario, nombre, correo, password_hash, estado_cuenta, rol_usuario) "
                        + "VALUES (?, ?, ?, 'hash', 'ACTIVA', "
                        + "(SELECT id_rol FROM \"Roles\" WHERE nombre = ? LIMIT 1))",
                id, "Usuario " + id, "usuario" + id + "@test.com", rol);
    }

    public static void insertarRecurso(JdbcTemplate jdbcTemplate, long id, long idProveedor, boolean activo) {
        jdbcTemplate.update("INSERT INTO \"Recursos\" (id_recurso, nombre, id_proveedor, activo) "
                        + "VALUES (?, ?, ?, ?)",
                id, "Recurso " + id, idProveedor, activo);
    }

    public static void insertarServicio(JdbcTemplate jdbcTemplate, long id, long idProveedor,
                                        long idRecurso, boolean activo) {
        jdbcTemplate.update("INSERT INTO \"Servicios\" "
                        + "(id_servicio, nombre, categoria, duracion, precio, usuario_proveedor, id_recursos, activo) "
                        + "VALUES (?, ?, 'QA', 30, 1000, ?, ?, ?)",
                id, "Servicio " + id, idProveedor, idRecurso, activo);
    }

    public static void insertarReserva(JdbcTemplate jdbcTemplate, long id, long idServicio,
                                       int estado, Instant horaInicio) {
        jdbcTemplate.update("INSERT INTO \"Reservas\" "
                        + "(id_reserva, servicio_reserva, estado_reserva, usuario_reserva, hora_inicio, "
                        + "valor_acordado, duracion_acordada) VALUES (?, ?, ?, ?, ?, 1000, 30)",
                id, idServicio, estado, PROVEEDOR, java.sql.Timestamp.from(horaInicio));
    }
}
