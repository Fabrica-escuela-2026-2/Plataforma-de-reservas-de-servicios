package com.udea.service_platform.modules.resources.repository;

import com.udea.service_platform.TestEmbeddedPostgres;
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

import java.util.List;

import static com.udea.service_platform.modules.resources.RecursoTestData.insertarRecurso;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class RecursoRepositoryTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararRecursos() {
        insertarRecurso(jdbcTemplate, 9721L, RecursoTestData.PROVEEDOR, true);
        jdbcTemplate.update("UPDATE \"Recursos\" SET nombre = ? WHERE id_recurso = ?", "Zebra", 9721L);
        insertarRecurso(jdbcTemplate, 9722L, RecursoTestData.PROVEEDOR, true);
        jdbcTemplate.update("UPDATE \"Recursos\" SET nombre = ? WHERE id_recurso = ?", "Alfombra", 9722L);
        insertarRecurso(jdbcTemplate, 9723L, RecursoTestData.OTRO_PROVEEDOR, true);
        jdbcTemplate.update("UPDATE \"Recursos\" SET nombre = ? WHERE id_recurso = ?", "Beta", 9723L);
    }

    @Test
    void findByIdProveedorOrderByNombreAsc_ordenaPorNombreYFiltraProveedor() {
        List<Long> ids = recursoRepository.findByIdProveedorOrderByNombreAsc(RecursoTestData.PROVEEDOR)
                .stream().map(recurso -> recurso.getIdRecursos()).toList();

        assertEquals(List.of(9722L, 9721L), ids);
    }

    @Test
    void findAllByOrderByNombreAsc_incluyeTodosLosProveedores() {
        List<Long> ids = recursoRepository.findAllByOrderByNombreAsc()
                .stream().map(recurso -> recurso.getIdRecursos()).toList();

        assertTrue(ids.indexOf(9722L) < ids.indexOf(9721L));
        assertTrue(ids.contains(9723L));
    }

    @Test
    void existsByIdProveedorAndNombreIgnoreCase_noDistingueMayusculasPeroSiProveedor() {
        assertTrue(recursoRepository.existsByIdProveedorAndNombreIgnoreCase(RecursoTestData.PROVEEDOR, "zebra"));
        assertTrue(recursoRepository.existsByIdProveedorAndNombreIgnoreCase(RecursoTestData.PROVEEDOR, "ALFOMBRA"));
        assertFalse(recursoRepository.existsByIdProveedorAndNombreIgnoreCase(RecursoTestData.OTRO_PROVEEDOR, "zebra"));
    }
}
