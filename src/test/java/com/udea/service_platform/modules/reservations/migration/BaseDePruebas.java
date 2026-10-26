package com.udea.service_platform.modules.reservations.migration;

import com.udea.service_platform.TestEmbeddedPostgres;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Bases aisladas dentro del PostgreSQL embebido de pruebas para la migracion
 * de hora_inicio: cada prueba crea su propia base y no toca la base
 * "postgres" que comparten las demas pruebas @SpringBootTest.
 */
final class BaseDePruebas {

    static final String USER = "postgres";
    static final String PASSWORD = "postgres";

    private BaseDePruebas() {
    }

    static String crearBase(String nombre) {
        String admin = TestEmbeddedPostgres.POSTGRES.getJdbcUrl(USER, "postgres");
        try (Connection connection = DriverManager.getConnection(admin, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + nombre);
            statement.execute("CREATE DATABASE " + nombre);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base de pruebas " + nombre, e);
        }
        return TestEmbeddedPostgres.POSTGRES.getJdbcUrl(USER, nombre);
    }

    /** Crea "Servicios" y "Reservas" con el esquema exacto de main. */
    static void aplicarEsquemaDeMain(String url) {
        try (InputStream in = BaseDePruebas.class.getResourceAsStream("/reservas-esquema-main.sql")) {
            if (in == null) {
                throw new IllegalStateException("No se encontro reservas-esquema-main.sql");
            }
            ejecutar(url, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer reservas-esquema-main.sql", e);
        }
    }

    static void ejecutar(String url, String sql) {
        try (Connection connection = conectar(url); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Fallo SQL de preparacion: " + sql, e);
        }
    }

    static long insertarDevolviendoId(String url, String sql) {
        try (Connection connection = conectar(url); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Fallo SQL de preparacion: " + sql, e);
        }
    }

    /** Valor de is_nullable de la columna, o null si la columna no existe. */
    static String nulabilidad(String url, String columna) {
        List<String> filas = consultar(url, "SELECT is_nullable FROM information_schema.columns "
                + "WHERE table_name = 'Reservas' AND column_name = '" + columna + "'");
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Cada fila como "id|hora_inicio en UTC|fecha_inicio". */
    static List<String> filasDeReservas(String url, boolean conFechaInicio) {
        String fechaInicio = conFechaInicio ? "fecha_inicio::text" : "''";
        return consultar(url, "SELECT id_reserva || '|' || coalesce(to_char(hora_inicio AT TIME ZONE 'UTC', "
                + "'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"'), 'null') || '|' || coalesce(" + fechaInicio + ", 'null') "
                + "FROM \"Reservas\" ORDER BY id_reserva");
    }

    static List<String> consultar(String url, String sql) {
        List<String> filas = new ArrayList<>();
        try (Connection connection = conectar(url); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                filas.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Fallo SQL de consulta: " + sql, e);
        }
        return filas;
    }

    static Connection conectar(String url) throws SQLException {
        return DriverManager.getConnection(url, USER, PASSWORD);
    }
}
