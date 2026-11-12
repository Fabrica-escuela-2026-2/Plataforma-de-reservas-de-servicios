package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.reservations.dto.ReservaRescheduleRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.dto.RescheduleSlotQuery;
import com.udea.service_platform.modules.reservations.event.ReservaReprogramadaEvent;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplRescheduleTest {

    private static final Long RESERVA_ID = 100L;
    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;
    private static final Long SERVICE_ID = 10L;

    private static final LocalDate FECHA_NUEVA = LocalDate.of(2026, 10, 21);
    private static final LocalTime HORA_NUEVA = LocalTime.of(10, 0);
    private static final Instant HORA_NUEVA_UTC = Instant.parse("2026-10-21T15:00:00Z");

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
    private ReservaReschedulePolicy reschedulePolicy;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    @Test
    void reprogramarReserva_actualizaSoloHoraInicioYPublicaEvento() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, 45);
        stubExito(reserva);

        ReservaResponse response = reservaService.rescheduleReservation(
                RESERVA_ID, request("2026-10-21", "10:00"), CLIENT_ID);

        assertEquals(HORA_NUEVA_UTC, reserva.getHoraInicio());
        assertEquals(EstadoReserva.ACTIVA.getId(), reserva.getIdEstado());
        assertEquals(CLIENT_ID, reserva.getIdUsuario());
        assertEquals(SERVICE_ID, reserva.getServicio().getId());
        assertEquals(new BigDecimal("25000.00"), reserva.getValorAcordado());
        assertEquals(45, reserva.getDuracionAcordada());

        assertEquals("ACTIVA", response.getEstado());
        assertEquals(FECHA_NUEVA, response.getFecha());
        assertEquals(HORA_NUEVA, response.getFechaInicio());
        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(45, response.getDuracionAcordada());

        verify(availabilityService).validateRescheduleSlot(argThat(query ->
                SERVICE_ID.equals(query.serviceId())
                        && PROVIDER_ID.equals(query.providerId())
                        && FECHA_NUEVA.equals(query.fecha())
                        && HORA_NUEVA.equals(query.horaInicio())
                        && query.duracionReserva() == 45
                        && query.duracionServicio() == 60
                        && RESERVA_ID.equals(query.excludeReservationId())));

        ArgumentCaptor<ReservaReprogramadaEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservaReprogramadaEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        ReservaReprogramadaEvent event = eventCaptor.getValue();
        assertEquals(RESERVA_ID, event.reservaId());
        assertEquals(CLIENT_ID, event.actorId());
        assertEquals(CLIENT_ID, event.clienteId());
        assertEquals(PROVIDER_ID, event.proveedorId());
        assertEquals(Instant.parse("2026-10-20T19:30:00Z"), event.horaAnterior());
        assertEquals(HORA_NUEVA_UTC, event.horaNueva());
        assertNotNull(event.timestamp());

        verify(reservaRepository).findByIdForUpdate(RESERVA_ID);
        verify(reservaRepository, never()).findById(any());
    }

    @Test
    void reprogramarReserva_sinDuracionAcordada_usaLaDuracionDelServicio() {
        Reserva reserva = reserva(EstadoReserva.CONFIRMADA, null);
        stubExito(reserva);

        reservaService.rescheduleReservation(
                RESERVA_ID, request("2026-10-21", "10:00"), PROVIDER_ID);

        verify(availabilityService).validateRescheduleSlot(argThat(query ->
                query.duracionReserva() == 60 && query.duracionServicio() == 60));
    }

    @Test
    void reprogramarReserva_sinDuracionValida_lanzaConflictYNoValidaFranja() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, null);
        reserva.getServicio().setDuracion(null);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reschedulePolicy.parseFecha("2026-10-21")).thenReturn(FECHA_NUEVA);
        when(reschedulePolicy.parseHora("10:00")).thenReturn(HORA_NUEVA);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.rescheduleReservation(
                        RESERVA_ID, request("2026-10-21", "10:00"), CLIENT_ID));

        assertEquals("La reserva no tiene una duración válida", ex.getMessage());
        verifyNoInteractions(availabilityService, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reprogramarReserva_inexistente_lanzaResourceNotFound() {
        when(reservaRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reservaService.rescheduleReservation(
                        999L, request("2026-10-21", "10:00"), CLIENT_ID));

        verifyNoInteractions(reschedulePolicy, availabilityService, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reprogramarReserva_conUsuarioAjeno_lanzaAccessDenied() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, 45);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        doThrow(new AccessDeniedException("No tienes permiso para reprogramar esta reserva"))
                .when(reschedulePolicy).validateActor(reserva, 99L);

        assertThrows(AccessDeniedException.class,
                () -> reservaService.rescheduleReservation(
                        RESERVA_ID, request("2026-10-21", "10:00"), 99L));

        verifyNoInteractions(availabilityService, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reprogramarReserva_enEstadoNoReprogramable_lanzaConflict() {
        Reserva reserva = reserva(EstadoReserva.EN_PROCESO, 45);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        doThrow(new ConflictException("La reserva no se puede reprogramar en estado EN_PROCESO"))
                .when(reschedulePolicy).validateState(reserva);

        assertThrows(ConflictException.class,
                () -> reservaService.rescheduleReservation(
                        RESERVA_ID, request("2026-10-21", "10:00"), CLIENT_ID));

        verifyNoInteractions(availabilityService, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reprogramarReserva_conFranjaNoDisponible_lanzaConflictYNoGuarda() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, 45);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reschedulePolicy.parseFecha("2026-10-21")).thenReturn(FECHA_NUEVA);
        when(reschedulePolicy.parseHora("10:00")).thenReturn(HORA_NUEVA);
        doThrow(new ConflictException("El horario seleccionado ya está ocupado"))
                .when(availabilityService).validateRescheduleSlot(any(RescheduleSlotQuery.class));

        assertThrows(ConflictException.class,
                () -> reservaService.rescheduleReservation(
                        RESERVA_ID, request("2026-10-21", "10:00"), CLIENT_ID));

        verify(reservaRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void reprogramarReserva_conFechaInvalida_lanzaIllegalArgumentYNoValidaFranja() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, 45);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reschedulePolicy.parseFecha("20/10/2026"))
                .thenThrow(new IllegalArgumentException(
                        "Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)"));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.rescheduleReservation(
                        RESERVA_ID, request("20/10/2026", "10:00"), CLIENT_ID));

        verifyNoInteractions(availabilityService, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reprogramarReserva_conHoraInvalida_lanzaIllegalArgumentYNoValidaFranja() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, 45);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reschedulePolicy.parseFecha("2026-10-21")).thenReturn(FECHA_NUEVA);
        when(reschedulePolicy.parseHora("25:99"))
                .thenThrow(new IllegalArgumentException(
                        "Formato de hora inválido. Use HH:mm (ej. 14:30)"));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.rescheduleReservation(
                        RESERVA_ID, request("2026-10-21", "25:99"), CLIENT_ID));

        verifyNoInteractions(availabilityService, applicationEventPublisher);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reprogramarReserva_sinHoraAnterior_publicaEventoConAnteriorNula() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, 45);
        reserva.setHoraInicio(null);
        stubExito(reserva);

        reservaService.rescheduleReservation(
                RESERVA_ID, request("2026-10-21", "10:00"), CLIENT_ID);

        ArgumentCaptor<ReservaReprogramadaEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservaReprogramadaEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertNull(eventCaptor.getValue().horaAnterior());
        assertEquals(HORA_NUEVA_UTC, eventCaptor.getValue().horaNueva());
    }

    private void stubExito(Reserva reserva) {
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(reschedulePolicy.parseFecha("2026-10-21")).thenReturn(FECHA_NUEVA);
        when(reschedulePolicy.parseHora("10:00")).thenReturn(HORA_NUEVA);
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(clientLookupService.findClientDisplayInfoByIds(Set.of(CLIENT_ID)))
                .thenReturn(Map.of(CLIENT_ID,
                        new ClientDisplayInfo(CLIENT_ID, "Cliente", "cliente@test.com")));
    }

    private ReservaRescheduleRequest request(String fecha, String hora) {
        return ReservaRescheduleRequest.builder()
                .fecha(fecha)
                .fechaInicio(hora)
                .build();
    }

    private Reserva reserva(EstadoReserva estado, Integer duracionAcordada) {
        return Reserva.builder()
                .id(RESERVA_ID)
                .idEstado(estado.getId())
                .idUsuario(CLIENT_ID)
                .servicio(servicio())
                .horaInicio(Instant.parse("2026-10-20T19:30:00Z"))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(duracionAcordada)
                .build();
    }

    private Service servicio() {
        return Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte")
                .precio(new BigDecimal("25000.00"))
                .duracion(60)
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
    }
}
