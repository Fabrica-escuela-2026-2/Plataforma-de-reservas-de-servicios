package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.reservations.service.ReservaLookupService;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.ProviderTypeRepository;
import com.udea.service_platform.modules.users.repository.RoleRepository;
import com.udea.service_platform.modules.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInactivationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ProviderTypeRepository providerTypeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ReservaLookupService reservaLookupService;

    @InjectMocks
    private UserServiceImpl userService;

    private Role clienteRole;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        clienteRole = Role.builder().idRol(1L).nombre("Cliente").build();
    }

    @Test
    void inactivateAccount_success() {
        User user = User.builder()
                .idUsuario(USER_ID)
                .nombre("Juan")
                .correo("juan@test.com")
                .password("hash")
                .estadoCuenta(User.ESTADO_ACTIVA)
                .role(clienteRole)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reservaLookupService.hasActiveReservations(USER_ID)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.inactivateAccount(USER_ID);

        assertEquals(User.ESTADO_INACTIVA, user.getEstadoCuenta());
        verify(userRepository).save(user);
        verify(reservaLookupService).hasActiveReservations(USER_ID);
    }

    @Test
    void inactivateAccount_hasActiveReservations_throwsIllegalArgument() {
        User user = User.builder()
                .idUsuario(USER_ID)
                .nombre("Juan")
                .correo("juan@test.com")
                .password("hash")
                .estadoCuenta(User.ESTADO_ACTIVA)
                .role(clienteRole)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(reservaLookupService.hasActiveReservations(USER_ID)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.inactivateAccount(USER_ID));
        assertEquals("No puedes inactivar tu cuenta porque tienes reservas activas. Cancela o reprograma las reservas pendientes antes de darte de baja.",
                ex.getMessage());
        assertEquals(User.ESTADO_ACTIVA, user.getEstadoCuenta());
        verify(userRepository, never()).save(any());
        verify(reservaLookupService).hasActiveReservations(USER_ID);
    }

    @Test
    void inactivateAccount_alreadyInactive_idempotent() {
        User user = User.builder()
                .idUsuario(USER_ID)
                .nombre("Juan")
                .correo("juan@test.com")
                .password("hash")
                .estadoCuenta(User.ESTADO_INACTIVA)
                .role(clienteRole)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.inactivateAccount(USER_ID);

        assertEquals(User.ESTADO_INACTIVA, user.getEstadoCuenta());
        verify(userRepository, never()).save(any());
        verify(reservaLookupService, never()).hasActiveReservations(any());
    }

    @Test
    void inactivateAccount_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.inactivateAccount(USER_ID));
        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(reservaLookupService, never()).hasActiveReservations(any());
    }
}