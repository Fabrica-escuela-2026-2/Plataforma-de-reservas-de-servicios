package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountLookupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountLookupServiceImpl accountLookupService;

    @Test
    void isAccountActive_conCuentaActiva_devuelveTrue() {
        User user = User.builder().idUsuario(5L).estadoCuenta(User.ESTADO_ACTIVA).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertTrue(accountLookupService.isAccountActive(5L));
    }

    @Test
    void isAccountActive_conCuentaInactiva_devuelveFalse() {
        User user = User.builder().idUsuario(5L).estadoCuenta(User.ESTADO_INACTIVA).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertFalse(accountLookupService.isAccountActive(5L));
    }

    @Test
    void isAccountActive_conUsuarioInexistente_devuelveFalse() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(accountLookupService.isAccountActive(99L));
    }

    @Test
    void isAccountActive_conIdNulo_devuelveFalseSinConsultar() {
        assertFalse(accountLookupService.isAccountActive(null));

        verifyNoInteractions(userRepository);
    }
}
