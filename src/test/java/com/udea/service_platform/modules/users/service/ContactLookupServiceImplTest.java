package com.udea.service_platform.modules.users.service;

import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactLookupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactLookupServiceImpl contactLookupService;

    @Test
    void findContactsByIds_devuelveNombreCompletoCorreoYTelefono() {
        User user = User.builder().idUsuario(5L).nombre("Pedro").apellido("Pérez")
                .correo("pedro@test.com").telefono("3001234567").build();
        when(userRepository.findAllById(Set.of(5L))).thenReturn(List.of(user));

        Map<Long, ContactInfo> result = contactLookupService.findContactsByIds(Set.of(5L));

        ContactInfo info = result.get(5L);
        assertEquals("Pedro Pérez", info.nombreCompleto());
        assertEquals("pedro@test.com", info.correo());
        assertEquals("3001234567", info.telefono());
    }

    @Test
    void findContactsByIds_sinApellidoNiTelefono_noEscribeNull() {
        User user = User.builder().idUsuario(6L).nombre("Ana").apellido(null)
                .correo("ana@test.com").build();
        when(userRepository.findAllById(Set.of(6L))).thenReturn(List.of(user));

        ContactInfo info = contactLookupService.findContactsByIds(Set.of(6L)).get(6L);

        assertEquals("Ana", info.nombreCompleto());
        assertNull(info.telefono());
    }

    @Test
    void findContactsByIds_conApellidoEnBlanco_loOmite() {
        User user = User.builder().idUsuario(7L).nombre("Luis").apellido("  ")
                .correo("luis@test.com").build();
        when(userRepository.findAllById(Set.of(7L))).thenReturn(List.of(user));

        assertEquals("Luis", contactLookupService.findContactsByIds(Set.of(7L)).get(7L).nombreCompleto());
    }

    @Test
    void findContactsByIds_conUsuarioInexistente_noLoIncluye() {
        when(userRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        assertTrue(contactLookupService.findContactsByIds(Set.of(99L)).isEmpty());
    }

    @Test
    void findContactsByIds_sinIds_noConsultaLaBase() {
        assertTrue(contactLookupService.findContactsByIds(Set.of()).isEmpty());
        assertTrue(contactLookupService.findContactsByIds(null).isEmpty());

        verifyNoInteractions(userRepository);
    }
}
