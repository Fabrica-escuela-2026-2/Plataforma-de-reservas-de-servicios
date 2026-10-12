package com.udea.service_platform.modules.users.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-05: executes the native EXISTS queries against the real schema.
 * Read-only assertions — no rows are written.
 */
@SpringBootTest
class UserRepositoryNativeQueryTest {

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
