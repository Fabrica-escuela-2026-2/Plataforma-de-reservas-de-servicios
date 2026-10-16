package com.udea.service_platform.modules.users.repository;

import com.udea.service_platform.TestEmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-05: executes the native EXISTS queries against the real schema.
 * Runs against an embedded PostgreSQL (no Docker, no developer database):
 * schema.sql/data.sql provision the fresh instance and test-schema.sql
 * guarantees the lookup tables + reference rows the assertions need.
 * Read-only assertions — no rows are written.
 */
@SpringBootTest
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class UserRepositoryNativeQueryTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void existsCiudad_withValidId_returnsTrue() {
        assertTrue(userRepository.existsCiudad(1L));
    }

    @Test
    void existsCiudad_withUnknownId_returnsFalse() {
        assertFalse(userRepository.existsCiudad(999999L));
    }

    @Test
    void existsTipoDocumento_withValidId_returnsTrue() {
        assertTrue(userRepository.existsTipoDocumento(1L));
    }

    @Test
    void existsTipoDocumento_withUnknownId_returnsFalse() {
        assertFalse(userRepository.existsTipoDocumento(999999L));
    }
}
