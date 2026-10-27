
package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceUpdateTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private Service existingService;
    private ServiceRequest updateRequest;

    @BeforeEach
    void setUp() {
        existingService = Service.builder()
                .id(10L)
                .nombre("Corte")
                .descripcion("Barbería")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(true)
                .build();

        updateRequest = ServiceRequest.builder()
                .nombre("Manicure")
                .descripcion("Manicure con esmaltado")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("35000.00"))
                .idRecursos(2L)
                .build();
    }

    @Test
    void updateService_actualizaSoloCamposEditables_idProveedorActivoIntactos() {
        // Arrange
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        when(recursoRepository.existsById(2L))
                .thenReturn(true);
        when(serviceRepository.save(any(Service.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        ServiceResponse response =
                serviceService.updateService(10L, updateRequest, 5L);

        // Assert: comprobar los campos actualizados en la respuesta
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Manicure", response.getNombre());
        assertEquals("Manicure con esmaltado", response.getDescripcion());
        assertEquals("Belleza", response.getCategoria());
        assertEquals(45, response.getDuracion());
        assertEquals(new BigDecimal("35000.00"), response.getPrecio());
        assertEquals(5L, response.getIdProveedor());
        assertEquals(2L, response.getIdRecursos());
        assertTrue(response.getActivo());

        // Assert: comprobar que la entidad guardada conserva los datos protegidos
        verify(serviceRepository).save(argThat(saved ->
                saved.getId().equals(10L)
                        && saved.getIdProveedor().equals(5L)
                        && Boolean.TRUE.equals(saved.getActivo())
                        && saved.getNombre().equals("Manicure")
                        && saved.getDescripcion().equals("Manicure con esmaltado")
                        && saved.getCategoria().equals("Belleza")
                        && saved.getDuracion().equals(45)
                        && saved.getPrecio().compareTo(
                                new BigDecimal("35000.00")) == 0
                        && saved.getIdRecursos().equals(2L)
        ));

        verify(serviceRepository).findById(10L);
        verify(recursoRepository).existsById(2L);
    }

    @Test
    void updateService_servicioInexistente_lanzaExcepcionNoLlamaSave() {
        // Arrange
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> serviceService.updateService(10L, updateRequest, 5L)
        );

        // Assert
        assertEquals("El servicio no existe", ex.getMessage());
        verify(serviceRepository).findById(10L);
        verify(recursoRepository, never()).existsById(any());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_otroProveedor_lanzaExcepcionNoLlamaSave() {
        // Arrange
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));

        // Act
        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> serviceService.updateService(10L, updateRequest, 7L)
        );

        // Assert
        assertEquals(
                "El servicio no pertenece al proveedor",
                ex.getMessage()
        );
        verify(serviceRepository).findById(10L);
        verify(recursoRepository, never()).existsById(any());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_recursoInexistente_lanzaExcepcionNoLlamaSave() {
        // Arrange
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        when(recursoRepository.existsById(2L))
                .thenReturn(false);

        // Act
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> serviceService.updateService(10L, updateRequest, 5L)
        );

        // Assert
        assertEquals(
                "El recurso seleccionado no existe",
                ex.getMessage()
        );
        verify(serviceRepository).findById(10L);
        verify(recursoRepository).existsById(2L);
        verify(serviceRepository, never()).save(any());
    }
}
