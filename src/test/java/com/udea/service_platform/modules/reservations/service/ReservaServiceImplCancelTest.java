package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplCancelTest {

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
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    @Test
    void cancelarReserva_actualizaEstadoPublicaEventoYConservaValores() {
        Reserva reserva = reserva();
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(cancellationPolicy.resolveActor(reserva, CLIENT_ID))
                .thenReturn(ReservaCancellationPolicy.CancellationActor.CLIENT);
        doNothing().when(cancellationPolicy).validateCancellation(
                reserva, ReservaCancellationPolicy.CancellationActor.CLIENT);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(clientLookupService.findClientDisplayInfoByIds(Set.of(CLIENT_ID)))
                .thenReturn(Map.of(CLIENT_ID,
                        new ClientDisplayInfo(CLIENT_ID, "Cliente", "cliente@test.com")));

        ReservaResponse response = reservaService.cancelReservation(RESERVA_ID, CLIENT_ID);

        assertEquals("CANCELADA", response.getEstado());
        assertEquals(EstadoReserva.CANCELADA.getId(), reserva.getIdEstado());
        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(30, response.getDuracionAcordada());
        assertEquals("Cliente", response.getClienteNombre());

        ArgumentCaptor<ReservaEstadoCambiadoEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservaEstadoCambiadoEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        ReservaEstadoCambiadoEvent event = eventCaptor.getValue();
        assertEquals(RESERVA_ID, event.reservaId());
        assertEquals(CLIENT_ID, event.actorId());
        assertEquals(EstadoReserva.ACTIVA, event.estadoAnterior());
        assertEquals(EstadoReserva.CANCELADA, event.estadoNuevo());
        verify(reservaRepository).findByIdForUpdate(RESERVA_ID);
        verify(reservaRepository, never()).findById(any());
    }

    @Test
    void cancelarReserva_inexistente_lanzaResourceNotFound() {
        when(reservaRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservaService.cancelReservation(999L, CLIENT_ID));

        verifyNoInteractions(cancellationPolicy, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarReserva_conConflictoDePolitica_noGuardaNiPublica() {
        Reserva reserva = reserva();
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(cancellationPolicy.resolveActor(reserva, CLIENT_ID))
                .thenThrow(new ConflictException("La reserva no se puede cancelar"));

        assertThrows(ConflictException.class,
                () -> reservaService.cancelReservation(RESERVA_ID, CLIENT_ID));

        verify(reservaRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private Reserva reserva() {
        return Reserva.builder()
                .id(RESERVA_ID)
                .idEstado(EstadoReserva.ACTIVA.getId())
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
