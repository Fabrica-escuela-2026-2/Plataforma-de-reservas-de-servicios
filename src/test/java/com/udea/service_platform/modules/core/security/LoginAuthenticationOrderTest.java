package com.udea.service_platform.modules.core.security;

import com.udea.service_platform.modules.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.function.Consumer;

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

    private UserDetails springSecurityUser(String correo,
            Consumer<org.springframework.security.core.userdetails.User.UserBuilder> personalizacion) {
        org.springframework.security.core.userdetails.User.UserBuilder builder =
                org.springframework.security.core.userdetails.User.withUsername(correo)
                        .password(passwordEncoder.encode(PASSWORD))
                        .authorities("ROLE_CLIENTE");
        personalizacion.accept(builder);
        return builder.build();
    }

    private AuthenticationManager authenticationManagerFor(UserDetails userDetails) {
        UserDetailsService userDetailsService = correo -> {
            if (userDetails.getUsername().equals(correo)) {
                return userDetails;
            }
            throw new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
        };
        return AuthenticationManagerFactory.create(userDetailsService, passwordEncoder);
    }

    private <T extends AuthenticationException> T assertAuthenticationFails(UserDetails userDetails,
            String password, Class<T> expectedException) {
        AuthenticationManager manager = authenticationManagerFor(userDetails);
        return assertThrows(expectedException, () -> manager.authenticate(
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), password)));
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

    @Test
    void lockedAccount_withCorrectPassword_throwsLocked() {
        UserDetails bloqueada = springSecurityUser("bloqueada@test.com",
                builder -> builder.accountLocked(true));

        assertAuthenticationFails(bloqueada, PASSWORD, LockedException.class);
    }

    @Test
    void expiredAccount_withCorrectPassword_throwsAccountExpired() {
        UserDetails expirada = springSecurityUser("expirada@test.com",
                builder -> builder.accountExpired(true));

        assertAuthenticationFails(expirada, PASSWORD, AccountExpiredException.class);
    }

    @Test
    void expiredCredentials_withCorrectPassword_throwsCredentialsExpired() {
        UserDetails credencialesExpiradas = springSecurityUser("credenciales-expiradas@test.com",
                builder -> builder.credentialsExpired(true));

        assertAuthenticationFails(credencialesExpiradas, PASSWORD, CredentialsExpiredException.class);
    }

    @Test
    void expiredCredentials_withWrongPassword_throwsBadCredentials() {
        UserDetails credencialesExpiradas = springSecurityUser("credenciales-expiradas-clave-mala@test.com",
                builder -> builder.credentialsExpired(true));

        assertAuthenticationFails(credencialesExpiradas, "ClaveEquivocada123!", BadCredentialsException.class);
    }
}
