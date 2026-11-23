package com.udea.service_platform.modules.resources.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.resources.RecursoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.udea.service_platform.modules.resources.RecursoTestData.insertarRecurso;
import static com.udea.service_platform.modules.resources.RecursoTestData.insertarReserva;
import static com.udea.service_platform.modules.resources.RecursoTestData.insertarServicio;
import static com.udea.service_platform.modules.resources.RecursoTestData.insertarUsuario;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-20: ejecuta el guard de baja contra el esquema real. Los estados que
 * ocupan agenda se leen de EstadoReserva.occupyingStateIds y los servicios
 * inactivos cuentan igual que los activos.
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ResourceReservationGuardImplTest {

    private static final Instant FUTURO = Instant.now().plus(10, ChronoUnit.DAYS);
    private static final Instant PASADO = Instant.now().minus(10, ChronoUnit.DAYS);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private ResourceReservationGuard guard;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararDatos() {
        insertarUsuario(jdbcTemplate, RecursoTestData.PROVEEDOR, "Proveedor de Servicios");
        for (long id = RecursoTestData.RECURSO; id <= RecursoTestData.RECURSO + 6; id++) {
            insertarRecurso(jdbcTemplate, id, RecursoTestData.PROVEEDOR, true);
        }
    }

    @Test
    void reservaActivaFutura_bloqueaLaBaja() {
        insertarServicio(jdbcTemplate, 9801L, RecursoTestData.PROVEEDOR, RecursoTestData.RECURSO, true);
        insertarReserva(jdbcTemplate, 9811L, 9801L, EstadoReserva.ACTIVA.getId(), FUTURO);

        assertTrue(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO, Instant.now()));
    }

    @Test
    void reservaEnProcesoFutura_bloqueaLaBaja() {
        insertarServicio(jdbcTemplate, 9802L, RecursoTestData.PROVEEDOR, RecursoTestData.RECURSO + 1, true);
        insertarReserva(jdbcTemplate, 9812L, 9802L, EstadoReserva.EN_PROCESO.getId(), FUTURO);

        assertTrue(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO + 1, Instant.now()));
    }

    @Test
    void reservaCanceladaFutura_noBloquea() {
        insertarServicio(jdbcTemplate, 9803L, RecursoTestData.PROVEEDOR, RecursoTestData.RECURSO + 2, true);
        insertarReserva(jdbcTemplate, 9813L, 9803L, EstadoReserva.CANCELADA.getId(), FUTURO);

        assertFalse(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO + 2, Instant.now()));
    }

    @Test
    void reservaNoAsistioFutura_noBloquea() {
        insertarServicio(jdbcTemplate, 9804L, RecursoTestData.PROVEEDOR, RecursoTestData.RECURSO + 3, true);
        insertarReserva(jdbcTemplate, 9814L, 9804L, EstadoReserva.NO_ASISTIO.getId(), FUTURO);

        assertFalse(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO + 3, Instant.now()));
    }

    @Test
    void reservaActivaPasada_noBloquea() {
        insertarServicio(jdbcTemplate, 9805L, RecursoTestData.PROVEEDOR, RecursoTestData.RECURSO + 4, true);
        insertarReserva(jdbcTemplate, 9815L, 9805L, EstadoReserva.ACTIVA.getId(), PASADO);

        assertFalse(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO + 4, Instant.now()));
    }

    @Test
    void reservaDeOtroRecurso_noBloquea() {
        insertarServicio(jdbcTemplate, 9806L, RecursoTestData.PROVEEDOR, RecursoTestData.OTRO_RECURSO, true);
        insertarReserva(jdbcTemplate, 9816L, 9806L, EstadoReserva.ACTIVA.getId(), FUTURO);

        assertFalse(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO, Instant.now()));
        assertTrue(guard.hasFutureOccupyingReservations(RecursoTestData.OTRO_RECURSO, Instant.now()));
    }

    @Test
    void reservaDeServicioInactivo_tambienBloquea() {
        insertarServicio(jdbcTemplate, 9807L, RecursoTestData.PROVEEDOR, RecursoTestData.RECURSO + 5, false);
        insertarReserva(jdbcTemplate, 9817L, 9807L, EstadoReserva.ACTIVA.getId(), FUTURO);

        assertTrue(guard.hasFutureOccupyingReservations(RecursoTestData.RECURSO + 5, Instant.now()));
    }
}
