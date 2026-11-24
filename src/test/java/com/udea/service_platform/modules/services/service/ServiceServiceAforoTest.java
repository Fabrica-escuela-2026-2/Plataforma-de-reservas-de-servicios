package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.service.ResourceLookupService;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceAforoTest {

    private static final Long SERVICE_ID = 9908L;
    private static final Long PROVIDER_ID = 9901L;
    private static final Long RECURSO_ID = 9907L;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ResourceLookupService resourceLookupService;

    @Mock
    private AforoChangeGuard aforoChangeGuard;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private Service servicio;

    @BeforeEach
    void setUp() {
        servicio = Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte")
                .descripcion("Barbería")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(PROVIDER_ID)
                .idRecursos(RECURSO_ID)
                .aforo(1)
                .activo(true)
                .build();
    }

    @Test
    void updateAforo_actualizaYRespetaElOrdenDeBloqueos() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(servicio));
        when(serviceRepository.findByIdForUpdate(SERVICE_ID)).thenReturn(Optional.of(servicio));
        when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceResponse response = serviceService.updateAforo(SERVICE_ID, 3, PROVIDER_ID);

        assertEquals(3, response.getAforo());
        InOrder orden = inOrder(serviceRepository, aforoChangeGuard);
        orden.verify(serviceRepository).findById(SERVICE_ID);
        orden.verify(aforoChangeGuard).lockResource(RECURSO_ID);
        orden.verify(serviceRepository).findByIdForUpdate(SERVICE_ID);
        orden.verify(aforoChangeGuard).validatePeak(SERVICE_ID, 45, 3);
        orden.verify(serviceRepository).save(servicio);
    }

    @Test
    void updateAforo_servicioInexistente_lanza404AntesDeBloquear() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> serviceService.updateAforo(SERVICE_ID, 2, PROVIDER_ID));

        assertEquals("El servicio no existe", ex.getMessage());
        verifyNoInteractions(aforoChangeGuard);
    }

    @Test
    void updateAforo_proveedorAjeno_lanza403AntesDeValidarElAforo() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(servicio));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> serviceService.updateAforo(SERVICE_ID, 0, 9999L));

        assertEquals("El servicio no pertenece al proveedor", ex.getMessage());
        verifyNoInteractions(aforoChangeGuard);
        verify(serviceRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void updateAforo_valorInvalido_lanza400AntesDeBloquear() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(servicio));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> serviceService.updateAforo(SERVICE_ID, 0, PROVIDER_ID));

        assertEquals("El aforo debe ser de al menos un cupo", ex.getMessage());
        verifyNoInteractions(aforoChangeGuard);
        verify(serviceRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void updateAforo_cambioDeRecursoEntreLocks_lanza409() {
        Service modificado = Service.builder()
                .id(SERVICE_ID)
                .duracion(45)
                .idProveedor(PROVIDER_ID)
                .idRecursos(9999L)
                .aforo(1)
                .build();
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(servicio));
        when(serviceRepository.findByIdForUpdate(SERVICE_ID)).thenReturn(Optional.of(modificado));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> serviceService.updateAforo(SERVICE_ID, 2, PROVIDER_ID));

        assertEquals("El servicio fue modificado, intenta de nuevo", ex.getMessage());
        verify(aforoChangeGuard, never()).validatePeak(any(), any(), any());
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateAforo_picoSuperaElNuevoAforo_lanza409YNoGuarda() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(servicio));
        when(serviceRepository.findByIdForUpdate(SERVICE_ID)).thenReturn(Optional.of(servicio));
        org.mockito.Mockito.doThrow(new ConflictException("Ya existen más reservas que el nuevo aforo"))
                .when(aforoChangeGuard).validatePeak(SERVICE_ID, 45, 1);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> serviceService.updateAforo(SERVICE_ID, 1, PROVIDER_ID));

        assertEquals("Ya existen más reservas que el nuevo aforo", ex.getMessage());
        verify(serviceRepository, never()).save(any());
    }
}
