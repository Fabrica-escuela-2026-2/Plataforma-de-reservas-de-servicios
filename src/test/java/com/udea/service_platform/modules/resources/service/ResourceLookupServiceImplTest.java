package com.udea.service_platform.modules.resources.service;

import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceLookupServiceImplTest {

    private static final Long ID_RECURSO = 9711L;
    private static final Long PROVEEDOR = 9701L;

    @Mock
    private RecursoRepository recursoRepository;

    private ResourceLookupService resourceLookupService;

    @BeforeEach
    void setUp() {
        resourceLookupService = new ResourceLookupServiceImpl(recursoRepository);
    }

    private Recurso recurso(boolean activo, Long idProveedor) {
        return Recurso.builder().idRecursos(ID_RECURSO).nombre("Taladro")
                .idProveedor(idProveedor).activo(activo).build();
    }

    @Test
    void noExiste_lanzaIllegalArgument() {
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resourceLookupService.validateLinkable(ID_RECURSO, PROVEEDOR));

        assertEquals("El recurso seleccionado no existe", ex.getMessage());
    }

    @Test
    void inactivo_lanzaIllegalArgument() {
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(Optional.of(recurso(false, PROVEEDOR)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resourceLookupService.validateLinkable(ID_RECURSO, PROVEEDOR));

        assertEquals("El recurso seleccionado no está activo", ex.getMessage());
    }

    @Test
    void deOtroProveedor_lanzaAccessDenied() {
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(Optional.of(recurso(true, 9702L)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> resourceLookupService.validateLinkable(ID_RECURSO, PROVEEDOR));

        assertEquals("El recurso no pertenece al proveedor", ex.getMessage());
    }

    @Test
    void activoYDelProveedor_permiteElVinculo() {
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(Optional.of(recurso(true, PROVEEDOR)));

        assertDoesNotThrow(() -> resourceLookupService.validateLinkable(ID_RECURSO, PROVEEDOR));
    }
}
