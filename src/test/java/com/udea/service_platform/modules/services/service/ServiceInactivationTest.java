package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceInactivationTest {

    private static final Long SERVICE_ID = 10L;
    private static final Long PROVIDER_ID = 5L;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private Service activeService;

    @BeforeEach
    void setUp() {
        activeService = Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte de cabello")
                .descripcion("Servicio de barbería")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idProveedor(PROVIDER_ID)
                .idRecursos(1L)
                .activo(true)
                .build();
    }

    @Test
    void inactivateService_ownedActiveService_setsInactiveAndSaves() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(activeService));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response = serviceService.inactivateService(SERVICE_ID, PROVIDER_ID);

        assertFalse(response.getActivo());
        assertEquals(SERVICE_ID, response.getId());
        assertFalse(activeService.getActivo());
        verify(serviceRepository).save(activeService);
    }

    @Test
    void inactivateService_alreadyInactive_isIdempotentAndDoesNotSave() {
        activeService.setActivo(false);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(activeService));

        ServiceResponse response = serviceService.inactivateService(SERVICE_ID, PROVIDER_ID);

        assertFalse(response.getActivo());
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void inactivateService_nonExistentService_throwsNotFound() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> serviceService.inactivateService(SERVICE_ID, PROVIDER_ID));

        assertEquals("El servicio no existe", ex.getMessage());
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void inactivateService_serviceOfAnotherProvider_throwsAccessDenied() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(activeService));

        assertThrows(AccessDeniedException.class,
                () -> serviceService.inactivateService(SERVICE_ID, 99L));

        assertTrue(activeService.getActivo());
        verify(serviceRepository, never()).save(any(Service.class));
    }
}
