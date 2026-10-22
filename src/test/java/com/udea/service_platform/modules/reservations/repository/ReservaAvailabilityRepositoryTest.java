package com.udea.service_platform.modules.reservations.repository;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-14: valida la consulta de reservas activas de un servicio dentro de un
 * rango UTC. Corre contra PostgreSQL embebido con inserciones por JDBC directo
 * (esquema real de Hibernate) para controlar hora_inicio y filas nulas.
 */
@SpringBootTest
class ReservaAvailabilityRepositoryTest {

    private static final String JDBC_URL = TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres");
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    private static final long SERVICE_ID = 7001L;
    private static final long OTHER_SERVICE_ID = 7002L;
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @Autowired
    private ReservaRepository reservaRepository;

    private Instant inicioRango;
    private Instant finRango;

    @BeforeEach
    void prepararDatos() throws Exception {
        inicioRango = FECHA.atStartOfDay().toInstant(ZoneOffset.UTC);
        finRango = FECHA.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        try (Connection connection = abrirConexion(); Statement statement = connection.createStatement()) {
            limpiar(statement);
            insertarServicio(statement, SERVICE_ID);
            insertarServicio(statement, OTHER_SERVICE_ID);
            insertarReserva(statement, SERVICE_ID, EstadoReserva.ACTIVA.getId(), inicioRango.plusSeconds(3600));
            insertarReserva(statement, SERVICE_ID, EstadoReserva.CANCELADA.getId(), inicioRango.plusSeconds(7200));
            insertarReserva(statement, OTHER_SERVICE_ID, EstadoReserva.ACTIVA.getId(), inicioRango.plusSeconds(10800));
            insertarReservaNula(statement, SERVICE_ID);
        }
    }

    @Test
    void devuelveSoloReservasActivasDelServicioEnElRango() {
        List<Reserva> reservas = reservaRepository.findActiveReservationsInRange(
                SERVICE_ID, EstadoReserva.ACTIVA.getId(), inicioRango, finRango);

        assertEquals(1, reservas.size());
        assertEquals(inicioRango.plusSeconds(3600), reservas.get(0).getHoraInicio());
    }

    @Test
    void excluyeFilasConHoraInicioNula() {
        List<Reserva> reservas = reservaRepository.findActiveReservationsInRange(
                SERVICE_ID, EstadoReserva.ACTIVA.getId(), inicioRango, finRango);

        assertTrue(reservas.stream().allMatch(r -> r.getHoraInicio() != null));
    }

    @Test
    void listadoDelCliente_ordenaPorHoraDescYDejaLasNulasAlFinal() {
        Page<Reserva> page = reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(
                10L, PageRequest.of(0, 10));

        List<Reserva> reservas = page.getContent();
        assertEquals(4, reservas.size());
        assertEquals(inicioRango.plusSeconds(10800), reservas.get(0).getHoraInicio());
        assertEquals(inicioRango.plusSeconds(7200), reservas.get(1).getHoraInicio());
        assertEquals(inicioRango.plusSeconds(3600), reservas.get(2).getHoraInicio());
        assertNull(reservas.get(3).getHoraInicio());
    }

    @Test
    void listadoDelProveedor_ordenaPorHoraDescYDejaLasNulasAlFinal() {
        Page<Reserva> page = reservaRepository.findByProviderIdOrderByFechaInicioDesc(
                555L, PageRequest.of(0, 10));

        List<Reserva> reservas = page.getContent();
        assertEquals(4, reservas.size());
        assertEquals(inicioRango.plusSeconds(10800), reservas.get(0).getHoraInicio());
        assertEquals(inicioRango.plusSeconds(7200), reservas.get(1).getHoraInicio());
        assertEquals(inicioRango.plusSeconds(3600), reservas.get(2).getHoraInicio());
        assertNull(reservas.get(3).getHoraInicio());
    }

    private void insertarServicio(Statement statement, long serviceId) throws Exception {
        statement.execute("INSERT INTO \"Servicios\" "
                + "(id_servicio, nombre, categoria, duracion, precio, usuario_proveedor, id_recursos, activo) "
                + "VALUES (" + serviceId + ", 'Servicio " + serviceId + "', 'Cat', 30, 1000, 555, 1, true)");
    }

    private void insertarReserva(Statement statement, long serviceId, int estado, Instant horaInicio) throws Exception {
        statement.execute("INSERT INTO \"Reservas\" (estado_reserva, servicio_reserva, usuario_reserva, hora_inicio) "
                + "VALUES (" + estado + ", " + serviceId + ", 10, '" + horaInicio + "')");
    }

    private void insertarReservaNula(Statement statement, long serviceId) throws Exception {
        statement.execute("INSERT INTO \"Reservas\" (estado_reserva, servicio_reserva, usuario_reserva, hora_inicio) "
                + "VALUES (" + EstadoReserva.ACTIVA.getId() + ", " + serviceId + ", 10, NULL)");
    }

    private void limpiar(Statement statement) throws Exception {
        statement.execute("DELETE FROM \"Reservas\" WHERE servicio_reserva IN ("
                + SERVICE_ID + ", " + OTHER_SERVICE_ID + ")");
        statement.execute("DELETE FROM \"Servicios\" WHERE id_servicio IN ("
                + SERVICE_ID + ", " + OTHER_SERVICE_ID + ")");
    }

    private Connection abrirConexion() throws Exception {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }
}
