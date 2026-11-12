package com.udea.service_platform.modules.core.security;

import com.udea.service_platform.modules.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAuthenticationOrderTest {

    private static final String PASSWORD = "ClaveSegura123!";
    private static final String CORREO_ACTIVA = "activa@test.com";
    private static final String CORREO_INACTIVA = "inactiva@test.com";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        String passwordHash = passwordEncoder.encode(PASSWORD);
        UserDetails activa = new UserDetailsImpl(1L, CORREO_ACTIVA, passwordHash, "Activa", "CLIENTE",
                User.ESTADO_ACTIVA);
        UserDetails inactiva = new UserDetailsImpl(2L, CORREO_INACTIVA, passwordHash, "Inactiva", "CLIENTE",
                User.ESTADO_INACTIVA);
        Map<String, UserDetails> usuarios = Map.of(
                CORREO_ACTIVA, activa,
                CORREO_INACTIVA, inactiva);

        UserDetailsService userDetailsService = correo -> {
            UserDetails usuario = usuarios.get(correo);
            if (usuario == null) {
                throw new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
            }
            return usuario;
        };

        authenticationManager = AuthenticationManagerFactory.create(userDetailsService, passwordEncoder);
    }

    private Authentication authenticate(String correo, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(correo, password));
    }

    @Test
    void inactiveAccount_withWrongPassword_throwsBadCredentials() {
        assertThrows(BadCredentialsException.class,
                () -> authenticate(CORREO_INACTIVA, "ClaveEquivocada123!"));
    }

    @Test
    void inactiveAccount_withCorrectPassword_throwsDisabled() {
        DisabledException exception = assertThrows(DisabledException.class,
                () -> authenticate(CORREO_INACTIVA, PASSWORD));

        assertEquals("User is disabled", exception.getMessage());
    }

    @Test
    void unknownAccount_throwsBadCredentials() {
        assertThrows(BadCredentialsException.class,
                () -> authenticate("no-existe@test.com", PASSWORD));
    }

    @Test
    void activeAccount_withCorrectPassword_authenticates() {
        Authentication authentication = authenticate(CORREO_ACTIVA, PASSWORD);

        assertTrue(authentication.isAuthenticated());
        assertEquals(CORREO_ACTIVA, authentication.getName());
    }

    @Test
    void activeAccount_withWrongPassword_throwsBadCredentials() {
        assertThrows(BadCredentialsException.class,
                () -> authenticate(CORREO_ACTIVA, "ClaveEquivocada123!"));
    }
}
