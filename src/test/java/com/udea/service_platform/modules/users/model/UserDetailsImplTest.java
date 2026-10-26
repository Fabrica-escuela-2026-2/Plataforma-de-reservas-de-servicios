package com.udea.service_platform.modules.users.model;

import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDetailsImplTest {

    @Test
    void isEnabled_trueWithActiva() {
        UserDetailsImpl details = new UserDetailsImpl(1L, "test@test.com", "pass", "Test", "CLIENTE", User.ESTADO_ACTIVA);
        assertTrue(details.isEnabled());
    }

    @Test
    void isEnabled_falseWithInactiva() {
        UserDetailsImpl details = new UserDetailsImpl(1L, "test@test.com", "pass", "Test", "CLIENTE", User.ESTADO_INACTIVA);
        assertFalse(details.isEnabled());
    }

    @Test
    void fiveArgConstructor_defaultsToActiva() {
        UserDetailsImpl details = new UserDetailsImpl(1L, "test@test.com", "pass", "Test", "CLIENTE");
        assertTrue(details.isEnabled());
    }
}