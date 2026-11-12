package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.TestEmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static com.udea.service_platform.modules.services.ServiceTestData.insertarProveedor;
import static com.udea.service_platform.modules.services.ServiceTestData.insertarServicio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-06: executes the bulk deactivation update against the real schema.
 * Inserts providers and services with explicit high ids through JdbcTemplate
 * so shared sequences are not advanced, and rolls everything back.
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ServiceAdministrationRepositoryTest {

    private static final long PROVIDER_A = 9511L;
    private static final long PROVIDER_B = 9512L;
    private static final long PROVIDER_WITHOUT_SERVICES = 9599L;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararProveedores() {
        insertarProveedor(jdbcTemplate, PROVIDER_A, "proveedor-a-9511@test.com");
        insertarProveedor(jdbcTemplate, PROVIDER_B, "proveedor-b-9512@test.com");
    }

    @Test
    void desactivaSoloLosServiciosDelProveedorIndicado() {
        insertarServicio(jdbcTemplate, 9611L, PROVIDER_A, "Cat", true);
        insertarServicio(jdbcTemplate, 9612L, PROVIDER_A, "Cat", true);
        insertarServicio(jdbcTemplate, 9613L, PROVIDER_B, "Cat", true);

        int filas = serviceRepository.deactivateServicesByProvider(PROVIDER_A);

        assertEquals(2, filas);
        assertFalse(estaActivo(9611L));
        assertFalse(estaActivo(9612L));
        assertTrue(estaActivo(9613L));
    }

    @Test
    void esIdempotenteYDevuelveCeroCuandoYaEstanInactivos() {
        insertarServicio(jdbcTemplate, 9621L, PROVIDER_A, "Cat", true);

        assertEquals(1, serviceRepository.deactivateServicesByProvider(PROVIDER_A));
        assertEquals(0, serviceRepository.deactivateServicesByProvider(PROVIDER_A));
        assertFalse(estaActivo(9621L));
    }

    @Test
    void proveedorSinServiciosDevuelveCeroSinFallar() {
        int filas = serviceRepository.deactivateServicesByProvider(PROVIDER_WITHOUT_SERVICES);

        assertEquals(0, filas);
    }

    @Test
    void devuelveElNumeroDeFilasAfectadasIgnorandoLasYaInactivas() {
        insertarServicio(jdbcTemplate, 9631L, PROVIDER_A, "Cat", true);
        insertarServicio(jdbcTemplate, 9632L, PROVIDER_A, "Cat", true);
        insertarServicio(jdbcTemplate, 9633L, PROVIDER_A, "Cat", true);
        insertarServicio(jdbcTemplate, 9634L, PROVIDER_A, "Cat", false);

        int filas = serviceRepository.deactivateServicesByProvider(PROVIDER_A);

        assertEquals(3, filas);
    }

    private boolean estaActivo(long id) {
        Boolean activo = jdbcTemplate.queryForObject(
                "SELECT activo FROM \"Servicios\" WHERE id_servicio = ?", Boolean.class, id);
        return Boolean.TRUE.equals(activo);
    }
}
