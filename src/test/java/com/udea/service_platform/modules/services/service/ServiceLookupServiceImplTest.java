
package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceLookupServiceImplTest {

    private static final Long SERVICE_ID = 10L;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceLookupServiceImpl serviceLookupService;

    @Test
    void findActiveService_conServicioActivo_devuelveServicio() {
        Service servicio = Service.builder()
                .id(SERVICE_ID)
                .nombre("Consulta general")
                .categoria("Salud")
                .duracion(60)
                .precio(new java.math.BigDecimal("50000"))
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(true)
                .build();

        when(serviceRepository.findById(SERVICE_ID))
                .thenReturn(Optional.of(servicio));

        Service resultado = serviceLookupService.findActiveService(SERVICE_ID);

        assertSame(servicio, resultado);
        assertTrue(resultado.getActivo());

        verify(serviceRepository).findById(SERVICE_ID);
        verifyNoMoreInteractions(serviceRepository);
    }

    @Test
    void findActiveService_conServicioInexistente_lanzaExcepcion() {
        when(serviceRepository.findById(SERVICE_ID))
                .thenReturn(Optional.empty());

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> serviceLookupService.findActiveService(SERVICE_ID)
        );

        assertEquals("El servicio no existe", excepcion.getMessage());

        verify(serviceRepository).findById(SERVICE_ID);
        verifyNoMoreInteractions(serviceRepository);
    }

    @Test
    void findActiveService_conServicioInactivo_lanzaExcepcion() {
        Service servicio = Service.builder()
                .id(SERVICE_ID)
                .nombre("Consulta general")
                .categoria("Salud")
                .duracion(60)
                .precio(new java.math.BigDecimal("50000"))
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(false)
                .build();

        when(serviceRepository.findById(SERVICE_ID))
                .thenReturn(Optional.of(servicio));

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> serviceLookupService.findActiveService(SERVICE_ID)
        );

        assertEquals("El servicio no está activo", excepcion.getMessage());

        verify(serviceRepository).findById(SERVICE_ID);
        verifyNoMoreInteractions(serviceRepository);
    }
}
