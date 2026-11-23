package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.resources.RecursoTestData;
import com.udea.service_platform.modules.resources.service.LinkedService;
import com.udea.service_platform.modules.resources.service.LinkedServicesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.udea.service_platform.modules.resources.RecursoTestData.insertarRecurso;
import static com.udea.service_platform.modules.resources.RecursoTestData.insertarServicio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-20: el proveedor de servicios vinculados agrupa en una sola consulta
 * por lote y devuelve DTOs, sin exponer entidades del modulo services.
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class LinkedServicesProviderImplTest {

    private static final long RECURSO_A = 9731L;
    private static final long RECURSO_B = 9732L;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private LinkedServicesProvider linkedServicesProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararDatos() {
        insertarRecurso(jdbcTemplate, RECURSO_A, RecursoTestData.PROVEEDOR, true);
        insertarRecurso(jdbcTemplate, RECURSO_B, RecursoTestData.PROVEEDOR, true);
        insertarServicio(jdbcTemplate, 9821L, RecursoTestData.PROVEEDOR, RECURSO_A, true);
        jdbcTemplate.update("UPDATE \"Servicios\" SET nombre = ? WHERE id_servicio = ?", "Servicio A", 9821L);
        insertarServicio(jdbcTemplate, 9822L, RecursoTestData.PROVEEDOR, RECURSO_A, false);
        jdbcTemplate.update("UPDATE \"Servicios\" SET nombre = ? WHERE id_servicio = ?", "Servicio B", 9822L);
        insertarServicio(jdbcTemplate, 9823L, RecursoTestData.PROVEEDOR, RECURSO_B, true);
        jdbcTemplate.update("UPDATE \"Servicios\" SET nombre = ? WHERE id_servicio = ?", "Servicio C", 9823L);
    }

    @Test
    void findLinkedServices_agrupaLosServiciosPorRecursoEnUnLote() {
        Map<Long, List<LinkedService>> resultado =
                linkedServicesProvider.findLinkedServices(List.of(RECURSO_A, RECURSO_B));

        assertEquals(2, resultado.get(RECURSO_A).size());
        assertTrue(resultado.get(RECURSO_A).contains(new LinkedService(9821L, "Servicio A", true)));
        assertTrue(resultado.get(RECURSO_A).contains(new LinkedService(9822L, "Servicio B", false)));
        assertEquals(List.of(new LinkedService(9823L, "Servicio C", true)), resultado.get(RECURSO_B));
    }

    @Test
    void findLinkedServices_sinIds_devuelveMapaVacio() {
        assertTrue(linkedServicesProvider.findLinkedServices(List.of()).isEmpty());
    }
}
