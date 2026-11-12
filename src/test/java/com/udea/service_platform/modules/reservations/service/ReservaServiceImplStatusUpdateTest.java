package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.dto.ReservaStatusUpdateRequest;
import com.udea.service_platform.modules.reservations.event.ReservaEstadoCambiadoEvent;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import com.udea.service_platform.modules.users.service.ClientDisplayInfo;
import com.udea.service_platform.modules.users.service.ClientLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplStatusUpdateTest {

    private static final Long RESERVA_ID = 100L;
    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ServiceLookupService serviceLookupService;

    @Mock
    private ClientLookupService clientLookupService;

    @Mock
    private ReservaCancellationPolicy cancellationPolicy;

    @Mock
    private ReservaStatusTransitionPolicy statusTransitionPolicy;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    @Test
    void actualizarEstado_actualizaGuardaPublicaEventoYDevuelveRespuesta() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(statusTransitionPolicy.parseTarget("CONFIRMADA"))
                .thenReturn(EstadoReserva.CONFIRMADA);
        doNothing().when(statusTransitionPolicy).validate(reserva, EstadoReserva.CONFIRMADA);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(clientLookupService.findClientDisplayInfoByIds(Set.of(CLIENT_ID)))
                .thenReturn(Map.of(CLIENT_ID,
                        new ClientDisplayInfo(CLIENT_ID, "Cliente", "cliente@test.com")));

        ReservaResponse response = reservaService.updateStatus(
                RESERVA_ID, request("CONFIRMADA"), PROVIDER_ID);

        assertEquals("CONFIRMADA", response.getEstado());
        assertEquals(EstadoReserva.CONFIRMADA.getId(), reserva.getIdEstado());
        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(30, response.getDuracionAcordada());
        assertEquals("Cliente", response.getClienteNombre());

        ArgumentCaptor<ReservaEstadoCambiadoEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservaEstadoCambiadoEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        ReservaEstadoCambiadoEvent event = eventCaptor.getValue();
        assertEquals(RESERVA_ID, event.reservaId());
        assertEquals(PROVIDER_ID, event.actorId());
        assertEquals(EstadoReserva.ACTIVA, event.estadoAnterior());
        assertEquals(EstadoReserva.CONFIRMADA, event.estadoNuevo());
        assertNotNull(event.timestamp());

        verify(reservaRepository).findByIdForUpdate(RESERVA_ID);
        verify(reservaRepository, never()).findById(any());
    }

    @Test
    void actualizarEstado_conservaElEstadoAnteriorEnElEvento() {
        Reserva reserva = reserva(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(statusTransitionPolicy.parseTarget("EN_PROCESO"))
                .thenReturn(EstadoReserva.EN_PROCESO);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(clientLookupService.findClientDisplayInfoByIds(Set.of(CLIENT_ID)))
                .thenReturn(Map.of(CLIENT_ID,
                        new ClientDisplayInfo(CLIENT_ID, "Cliente", "cliente@test.com")));

        reservaService.updateStatus(RESERVA_ID, request("EN_PROCESO"), PROVIDER_ID);

        ArgumentCaptor<ReservaEstadoCambiadoEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservaEstadoCambiadoEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(EstadoReserva.CONFIRMADA, eventCaptor.getValue().estadoAnterior());
        assertEquals(EstadoReserva.EN_PROCESO, eventCaptor.getValue().estadoNuevo());
        assertEquals(EstadoReserva.EN_PROCESO.getId(), reserva.getIdEstado());
    }

    @Test
    void actualizarEstado_inexistente_lanzaResourceNotFound() {
        when(reservaRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservaService.updateStatus(999L, request("CONFIRMADA"), PROVIDER_ID));

        verifyNoInteractions(statusTransitionPolicy, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void actualizarEstado_conProveedorAjeno_lanzaAccessDenied() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class,
                () -> reservaService.updateStatus(RESERVA_ID, request("CONFIRMADA"), 99L));

        verifyNoInteractions(statusTransitionPolicy, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void actualizarEstado_sinServicio_lanzaAccessDenied() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        reserva.setServicio(null);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class,
                () -> reservaService.updateStatus(RESERVA_ID, request("CONFIRMADA"), PROVIDER_ID));

        verifyNoInteractions(statusTransitionPolicy, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void actualizarEstado_conTransicionInvalida_lanzaConflictYNoGuarda() {
        Reserva reserva = reserva(EstadoReserva.COMPLETADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(statusTransitionPolicy.parseTarget("EN_PROCESO"))
                .thenReturn(EstadoReserva.EN_PROCESO);
        doThrow(new ConflictException("No se puede cambiar el estado de una reserva en estado COMPLETADA"))
                .when(statusTransitionPolicy).validate(reserva, EstadoReserva.EN_PROCESO);

        assertThrows(ConflictException.class,
                () -> reservaService.updateStatus(RESERVA_ID, request("EN_PROCESO"), PROVIDER_ID));

        verify(reservaRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void actualizarEstado_conEstadoDesconocido_lanzaIllegalArgumentYNoGuarda() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(statusTransitionPolicy.parseTarget("PAUSADA"))
                .thenThrow(new IllegalArgumentException("Estado no válido: 'PAUSADA'"));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.updateStatus(RESERVA_ID, request("PAUSADA"), PROVIDER_ID));

        verify(statusTransitionPolicy, never()).validate(any(), any());
        verify(reservaRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private ReservaStatusUpdateRequest request(String estado) {
        return ReservaStatusUpdateRequest.builder().estado(estado).build();
    }

    private Reserva reserva(EstadoReserva estado) {
        return Reserva.builder()
                .id(RESERVA_ID)
                .idEstado(estado.getId())
                .idUsuario(CLIENT_ID)
                .servicio(servicio())
                .horaInicio(Instant.parse("2026-10-20T19:30:00Z"))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
    }

    private Service servicio() {
        return Service.builder()
                .id(10L)
                .nombre("Corte")
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
    }
}
