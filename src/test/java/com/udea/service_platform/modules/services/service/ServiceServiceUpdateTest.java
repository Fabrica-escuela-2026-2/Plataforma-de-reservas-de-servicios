
package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.categories.model.Categoria;
import com.udea.service_platform.modules.categories.service.CategoriaInfo;
import com.udea.service_platform.modules.categories.service.CategoriaLookupService;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.service.ResourceLookupService;
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
    private ResourceLookupService resourceLookupService;

    @Mock
    private CategoriaLookupService categoriaLookupService;

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
                .categoria(Categoria.builder().id(1L).nombre("Belleza").build())
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
        doNothing().when(resourceLookupService).validateLinkable(2L, 5L);
        when(categoriaLookupService.resolveOrCreate("Belleza"))
                .thenReturn(new CategoriaInfo(1L, "Belleza"));
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
                        && saved.getCategoria().getNombre().equals("Belleza")
                        && saved.getDuracion().equals(45)
                        && saved.getPrecio().compareTo(
                                new BigDecimal("35000.00")) == 0
                        && saved.getIdRecursos().equals(2L)
        ));

        verify(serviceRepository).findById(10L);
        verify(resourceLookupService).validateLinkable(2L, 5L);
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
        verify(resourceLookupService, never()).validateLinkable(any(), any());
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
        verify(resourceLookupService, never()).validateLinkable(any(), any());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_recursoInexistente_lanzaExcepcionNoLlamaSave() {
        // Arrange
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        doThrow(new IllegalArgumentException("El recurso seleccionado no existe"))
                .when(resourceLookupService).validateLinkable(2L, 5L);

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
        verify(resourceLookupService).validateLinkable(2L, 5L);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_mismoRecursoInactivo_noValidaYActualiza() {
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        when(categoriaLookupService.resolveOrCreate("Belleza"))
                .thenReturn(new CategoriaInfo(1L, "Belleza"));
        when(serviceRepository.save(any(Service.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response =
                serviceService.updateService(10L, requestConRecurso(1L), 5L);

        assertEquals(1L, response.getIdRecursos());
        assertEquals("Manicure", response.getNombre());
        verify(resourceLookupService, never()).validateLinkable(any(), any());
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void updateService_cambiaARecursoInactivo_lanza400() {
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        doThrow(new IllegalArgumentException("El recurso seleccionado no está activo"))
                .when(resourceLookupService).validateLinkable(2L, 5L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> serviceService.updateService(10L, requestConRecurso(2L), 5L)
        );

        assertEquals("El recurso seleccionado no está activo", ex.getMessage());
        verify(resourceLookupService).validateLinkable(2L, 5L);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_cambiaARecursoDeOtroProveedor_lanza403() {
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        doThrow(new AccessDeniedException("El recurso no pertenece al proveedor"))
                .when(resourceLookupService).validateLinkable(2L, 5L);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> serviceService.updateService(10L, requestConRecurso(2L), 5L)
        );

        assertEquals("El recurso no pertenece al proveedor", ex.getMessage());
        verify(resourceLookupService).validateLinkable(2L, 5L);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_cambiaARecursoPropioActivo_actualiza() {
        when(serviceRepository.findById(10L))
                .thenReturn(Optional.of(existingService));
        when(categoriaLookupService.resolveOrCreate("Belleza"))
                .thenReturn(new CategoriaInfo(1L, "Belleza"));
        when(serviceRepository.save(any(Service.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response =
                serviceService.updateService(10L, requestConRecurso(2L), 5L);

        assertEquals(2L, response.getIdRecursos());
        verify(resourceLookupService).validateLinkable(2L, 5L);
        verify(serviceRepository).save(any(Service.class));
    }

    private static ServiceRequest requestConRecurso(long idRecursos) {
        return ServiceRequest.builder()
                .nombre("Manicure")
                .descripcion("Manicure con esmaltado")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("35000.00"))
                .idRecursos(idRecursos)
                .build();
    }
}
