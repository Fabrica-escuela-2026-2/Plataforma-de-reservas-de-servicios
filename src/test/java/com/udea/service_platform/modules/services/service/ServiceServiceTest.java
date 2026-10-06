package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.resources.repository.RecursoRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @InjectMocks
    private ServiceService serviceService;

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
        when(recursoRepository.existsById(1L)).thenReturn(true);

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
        when(serviceRepository.save(any(Service.class))).thenReturn(savedService);

        ServiceResponse response = serviceService.createService(request, 5L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Corte de cabello", response.getNombre());
        assertEquals("Belleza", response.getCategoria());
        assertEquals(30, response.getDuracion());
        assertEquals(new BigDecimal("25.00"), response.getPrecio());
        assertEquals(5L, response.getIdProveedor());
        assertEquals(1L, response.getIdRecursos());
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void createService_withNonExistentRecurso_throwsIllegalArgument() {
        request.setIdRecursos(999999L);
        when(recursoRepository.existsById(999999L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceService.createService(request, 5L)
        );

        assertEquals("El recurso seleccionado no existe", exception.getMessage());
        verify(serviceRepository, never()).save(any());
    }
}