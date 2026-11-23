package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.resources.service.ResourceLookupService;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ResourceLookupService resourceLookupService;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private ServiceRequest request;

    @BeforeEach
    void setUp() {
        request = ServiceRequest.builder()
                .nombre("Corte de cabello")
                .descripcion("Servicio de barbería")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idRecursos(1L)
                .build();
    }

    @Test
    void createService_withExistingRecurso_success() {
        // Arrange
        doNothing().when(resourceLookupService).validateLinkable(1L, 5L);

        Service savedService = Service.builder()
                .id(10L)
                .nombre("Corte de cabello")
                .descripcion("Servicio de barbería")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idProveedor(5L)
                .idRecursos(1L)
                .build();

        when(serviceRepository.save(any(Service.class)))
                .thenReturn(savedService);

        // Act
        ServiceResponse response = serviceService.createService(request, 5L);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Corte de cabello", response.getNombre());
        assertEquals("Belleza", response.getCategoria());
        assertEquals(30, response.getDuracion());
        assertEquals(new BigDecimal("25.00"), response.getPrecio());
        assertEquals(5L, response.getIdProveedor());
        assertEquals(1L, response.getIdRecursos());

        verify(resourceLookupService).validateLinkable(1L, 5L);
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void createService_withNonExistentRecurso_throwsIllegalArgument() {
        // Arrange
        request.setIdRecursos(999999L);
        doThrow(new IllegalArgumentException("El recurso seleccionado no existe"))
                .when(resourceLookupService).validateLinkable(999999L, 5L);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceService.createService(request, 5L)
        );

        // Assert
        assertEquals(
                "El recurso seleccionado no existe",
                exception.getMessage()
        );
        verify(resourceLookupService).validateLinkable(999999L, 5L);
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void createService_assignsProviderAndRequestFieldsToEntity() {
        // Arrange
        Long providerId = 8L;

        doNothing().when(resourceLookupService).validateLinkable(1L, providerId);
        when(serviceRepository.save(any(Service.class)))
                .thenAnswer(invocation -> {
                    Service service = invocation.getArgument(0);
                    service.setId(20L);
                    return service;
                });

        // Act
        ServiceResponse response =
                serviceService.createService(request, providerId);

        // Assert
        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals(providerId, response.getIdProveedor());
        assertEquals(request.getNombre(), response.getNombre());
        assertEquals(request.getDescripcion(), response.getDescripcion());
        assertEquals(request.getCategoria(), response.getCategoria());
        assertEquals(request.getDuracion(), response.getDuracion());
        assertEquals(0, request.getPrecio().compareTo(response.getPrecio()));
        assertEquals(request.getIdRecursos(), response.getIdRecursos());

        verify(resourceLookupService).validateLinkable(1L, providerId);
        verify(serviceRepository).save(argThat(service ->
                providerId.equals(service.getIdProveedor())
                        && request.getNombre().equals(service.getNombre())
                        && request.getDescripcion().equals(
                                service.getDescripcion())
                        && request.getCategoria().equals(
                                service.getCategoria())
                        && request.getDuracion().equals(service.getDuracion())
                        && request.getPrecio().compareTo(
                                service.getPrecio()) == 0
                        && request.getIdRecursos().equals(
                                service.getIdRecursos())
        ));
    }
}