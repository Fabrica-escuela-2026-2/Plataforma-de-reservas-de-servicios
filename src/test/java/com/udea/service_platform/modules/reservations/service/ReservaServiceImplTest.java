package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.service.NotificationService;
import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.AvailabilitySlotResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import com.udea.service_platform.modules.users.service.AccountLookupService;
import com.udea.service_platform.modules.users.service.ClientDisplayInfo;
import com.udea.service_platform.modules.users.service.ClientLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final Long SERVICE_ID = 10L;
    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;

    /** 2026-10-15 10:00 hora de Bogota (UTC-5). */
    private static final Instant AHORA = Instant.parse("2026-10-15T15:00:00Z");

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
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AccountLookupService accountLookupService;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private ReservationResourceLock resourceLock;

    @Mock
    private ReservationSlotPolicy slotPolicy;

    @Mock
    private NotificationService notificationService;

    private ReservaServiceImpl reservaService;

    @BeforeEach
    void setUp() {
        reservaService = crearServicio(AHORA);
    }

    private ReservaServiceImpl crearServicio(Instant ahora) {
        Clock clock = Clock.fixed(ahora, TimeConfig.ZONA_BOGOTA);
        return new ReservaServiceImpl(reservaRepository, serviceLookupService, clientLookupService,
                cancellationPolicy, statusTransitionPolicy, reschedulePolicy, applicationEventPublisher, accountLookupService,
                availabilityService, resourceLock, slotPolicy, notificationService, clock);
    }

    private void stubServicioDisponible(Service servicio) {
        when(serviceLookupService.findActiveService(SERVICE_ID)).thenReturn(servicio);
        when(serviceLookupService.lockActiveService(SERVICE_ID)).thenReturn(servicio);
        when(accountLookupService.isAccountActive(PROVIDER_ID)).thenReturn(true);
    }

    private void stubDisponibilidad(String fecha, EstadoSlot estado) {
        stubEvaluacion(LocalTime.of(14, 30), estado);
    }

    private void stubEvaluacion(LocalTime horaValida, EstadoSlot estado) {
        SlotDecision decision = switch (estado) {
            case LIBRE -> new SlotDecision(EstadoSlot.LIBRE, SlotReason.OK, 1, 1, null);
            case BLOQUEADA -> new SlotDecision(
                    EstadoSlot.BLOQUEADA, SlotReason.PROVIDER_BLOCK, 1, 0, null);
            case OCUPADA -> new SlotDecision(
                    EstadoSlot.OCUPADA, SlotReason.CAPACITY_FULL, 1, 0,
                    ReservationSlotPolicy.MSG_SIN_CUPOS);
        };
        when(slotPolicy.evaluate(any(), any(), any())).thenAnswer(invocacion ->
                horaValida.equals(invocacion.getArgument(1)) ? decision : decisionFueraHorario());
    }

    private SlotDecision decisionFueraHorario() {
        return new SlotDecision(EstadoSlot.BLOQUEADA, SlotReason.OUT_OF_SCHEDULE, 1, 0, null);
    }

    private void stubClienteYGuardado() {
        when(clientLookupService.findClientDisplayInfo(CLIENT_ID))
                .thenReturn(new ClientDisplayInfo(CLIENT_ID, "Cliente Uno", "cliente@test.com"));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> {
            Reserva reserva = invocacion.getArgument(0);
            reserva.setId(100L);
            return reserva;
        });
    }

    @Test
    void crearReserva_conHorarioLibre_guardaElInstantCorrectoYQuedaActiva() {
        stubServicioDisponible(servicio());
        stubDisponibilidad("2026-10-20", EstadoSlot.LIBRE);
        stubClienteYGuardado();

        ReservaResponse response = reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());

        assertEquals(Instant.parse("2026-10-20T19:30:00Z"), captor.getValue().getHoraInicio());
        assertEquals(EstadoReserva.ACTIVA.getId(), captor.getValue().getIdEstado());
        assertEquals(100L, response.getId());
        assertEquals(LocalTime.of(14, 30), response.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 20), response.getFecha());
        assertEquals("ACTIVA", response.getEstado());
        assertEquals("Cliente Uno", response.getClienteNombre());
    }

    @Test
    void crearReserva_conHorarioLibre_notificaAlClienteYAlProveedor() {
        stubServicioDisponible(servicio());
        stubDisponibilidad("2026-10-20", EstadoSlot.LIBRE);
        stubClienteYGuardado();

        reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID);

        ArgumentCaptor<String> mensajeCliente = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mensajeProveedor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(eq(CLIENT_ID), eq(TipoNotificacion.RESERVA_CONFIRMADA),
                mensajeCliente.capture());
        verify(notificationService).notifyUser(eq(PROVIDER_ID), eq(TipoNotificacion.NUEVA_RESERVA),
                mensajeProveedor.capture());

        assertTrue(mensajeCliente.getValue().contains("Corte"));
        assertTrue(mensajeCliente.getValue().contains("2026-10-20 a las 14:30"));
        assertTrue(mensajeProveedor.getValue().contains("Cliente Uno"));
        assertTrue(mensajeProveedor.getValue().contains("2026-10-20 a las 14:30"));
    }

    @Test
    void crearReserva_conPrecioYDuracionActuales_losGuardaComoValoresAcordados() {
        Service servicioActualizado = Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte")
                .precio(new BigDecimal("35000.00"))
                .duracion(45)
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
        stubServicioDisponible(servicioActualizado);
        stubDisponibilidad("2026-10-20", EstadoSlot.LIBRE);
        stubClienteYGuardado();

        ReservaResponse response = reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());

        assertEquals(new BigDecimal("35000.00"), captor.getValue().getValorAcordado());
        assertEquals(45, captor.getValue().getDuracionAcordada());
        assertEquals(new BigDecimal("35000.00"), response.getValorAcordado());
        assertEquals(45, response.getDuracionAcordada());
    }

    @Test
    void crearReserva_enElMinutoActual_noLaTrataComoPasada() {
        // 10:00:30 en Bogota: el slot de las 10:00 sigue siendo el del minuto en curso
        ReservaServiceImpl servicioConHoraActual = crearServicio(Instant.parse("2026-10-15T15:00:30Z"));
        stubServicioDisponible(servicio());
        stubEvaluacion(LocalTime.of(10, 0), EstadoSlot.LIBRE);
        stubClienteYGuardado();

        ReservaResponse response = servicioConHoraActual.createReservation(
                request("2026-10-15", "10:00"), CLIENT_ID);

        assertEquals(LocalTime.of(10, 0), response.getFechaInicio());
    }

    @Test
    void crearReserva_conHorarioOcupado_lanzaConflictoYNoCreaNada() {
        stubServicioDisponible(servicio());
        stubDisponibilidad("2026-10-20", EstadoSlot.OCUPADA);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID));

        assertEquals(ReservationSlotPolicy.MSG_SIN_CUPOS, ex.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void crearReserva_conHorarioBloqueadoPorElProveedor_noSePuedeReservar() {
        stubServicioDisponible(servicio());
        stubDisponibilidad("2026-10-20", EstadoSlot.BLOQUEADA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void crearReserva_fueraDelHorarioDeAtencion_noSePuedeReservar() {
        stubServicioDisponible(servicio());
        // la disponibilidad solo ofrece el slot de las 14:30; se pide uno fuera del horario
        stubDisponibilidad("2026-10-20", EstadoSlot.LIBRE);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-20", "23:00"), CLIENT_ID));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void crearReserva_enUnDiaSinHorarioConfigurado_noSePuedeReservar() {
        stubServicioDisponible(servicio());
        when(slotPolicy.evaluate(any(), any(), any())).thenReturn(decisionFueraHorario());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-25", "14:30"), CLIENT_ID));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void crearReserva_enUnaFechaPasada_noSePuedeReservarNiTomaElBloqueo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-14", "14:30"), CLIENT_ID));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        verifyNoInteractions(serviceLookupService, availabilityService, reservaRepository, notificationService);
    }

    @Test
    void crearReserva_enUnaHoraYaPasadaDeHoy_noSePuedeReservar() {
        // ahora son las 10:00 en Bogota: las 09:59 de hoy ya pasaron
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-15", "09:59"), CLIENT_ID));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        verifyNoInteractions(serviceLookupService, reservaRepository, notificationService);
    }

    @Test
    void crearReserva_conServicioNoDisponible_noCreaNada() {
        when(serviceLookupService.findActiveService(SERVICE_ID))
                .thenThrow(new IllegalArgumentException("El servicio no está disponible"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID));

        assertEquals("El servicio no está disponible", ex.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void crearReserva_conCuentaDelProveedorInactiva_elServicioNoEstaDisponible() {
        when(serviceLookupService.findActiveService(SERVICE_ID)).thenReturn(servicio());
        when(serviceLookupService.lockActiveService(SERVICE_ID)).thenReturn(servicio());
        when(accountLookupService.isAccountActive(PROVIDER_ID)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-20", "14:30"), CLIENT_ID));

        assertEquals("El servicio no está disponible", ex.getMessage());
        verifyNoInteractions(availabilityService, reservaRepository, notificationService);
    }

    @Test
    void crearReserva_conFormatoDeFechaInvalido_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("20/10/2026", "14:30"), CLIENT_ID));

        assertEquals("Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)", ex.getMessage());
        verifyNoInteractions(serviceLookupService, reservaRepository);
    }

    @Test
    void crearReserva_conFormatoDeHoraInvalido_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-20", "25:99"), CLIENT_ID));

        assertEquals("Formato de hora inválido. Use HH:mm (ej. 14:30)", ex.getMessage());
        verifyNoInteractions(serviceLookupService, reservaRepository);
    }

    @Test
    void crearReserva_delPropioProveedor_lanzaIllegalArgument() {
        stubServicioDisponible(servicio());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("2026-10-20", "14:30"), PROVIDER_ID));

        assertEquals("No puedes reservar tu propio servicio", ex.getMessage());
        verify(reservaRepository, never()).save(any(Reserva.class));
        verify(notificationService, never()).notifyUser(any(), any(), anyString());
    }

    @Test
    void listarReservasDelProveedor_convierteLaHoraLocalYElCliente() {
        // Arrange
        Reserva reserva = reservaExistente(2L);
        Page<Reserva> page = new PageImpl<>(List.of(reserva));

        when(reservaRepository.findByProviderIdOrderByFechaInicioDesc(
                eq(PROVIDER_ID), any()))
                .thenReturn(page);

        when(clientLookupService.findClientDisplayInfoByIds(any()))
                .thenReturn(Map.of(
                        2L,
                        new ClientDisplayInfo(
                                2L, "Cliente", "cliente@test.com")));

        // Act
        Page<ReservaResponse> result = reservaService.getProviderReservations(
                PROVIDER_ID, PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());

        ReservaResponse response = result.getContent().get(0);

        assertEquals(LocalTime.of(14, 30), response.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 20), response.getFecha());
        assertEquals("Cliente", response.getClienteNombre());
    }

    @Test
    void listarReservasDelProveedor_conClienteDesconocido_muestraUnknown() {
        // Arrange
        Reserva reserva = reservaExistente(2L);
        Page<Reserva> page = new PageImpl<>(List.of(reserva));

        when(reservaRepository.findByProviderIdOrderByFechaInicioDesc(
                eq(PROVIDER_ID), any()))
                .thenReturn(page);

        when(clientLookupService.findClientDisplayInfoByIds(any()))
                .thenReturn(Map.of());

        // Act
        Page<ReservaResponse> result = reservaService.getProviderReservations(
                PROVIDER_ID, PageRequest.of(0, 10));

        // Assert
        assertEquals("Unknown", result.getContent().get(0).getClienteNombre());
    }

    @Test
    void listarReservasDelCliente_devuelveElResumenConFecha() {
        // Arrange
        Reserva reserva = reservaExistente(2L);

        when(reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(
                eq(CLIENT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(reserva)));

        when(clientLookupService.findClientDisplayInfo(CLIENT_ID))
                .thenReturn(new ClientDisplayInfo(
                        CLIENT_ID, "Cliente", "cliente@test.com"));

        // Act
        Page<ReservaSummaryResponse> result = reservaService.getClientReservations(
                CLIENT_ID, PageRequest.of(0, 10));

        // Assert
        ReservaSummaryResponse summary = result.getContent().get(0);

        assertEquals(LocalTime.of(14, 30), summary.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 20), summary.getFecha());
    }

    private Reserva reservaExistente(Long idUsuario) {
        return Reserva.builder()
                .id(100L)
                .idEstado(EstadoReserva.ACTIVA.getId())
                .idUsuario(idUsuario)
                .servicio(servicio())
                .horaInicio(Instant.parse("2026-10-20T19:30:00Z"))
                .build();
    }

    private AvailabilitySlotResponse slot(int hora, int minuto, EstadoSlot estado) {
        return AvailabilitySlotResponse.builder()
                .horaInicio(LocalTime.of(hora, minuto))
                .horaFin(LocalTime.of(hora, minuto).plusMinutes(30))
                .estado(estado)
                .build();
    }

    private AvailabilityResponse disponibilidad(String fecha, AvailabilitySlotResponse... slots) {
        return AvailabilityResponse.builder()
                .fecha(LocalDate.parse(fecha))
                .idServicio(SERVICE_ID)
                .duracionMinutos(30)
                .slots(List.of(slots))
                .build();
    }

    private ReservaRequest request(String fecha, String hora) {
        return ReservaRequest.builder()
                .idServicio(SERVICE_ID)
                .fecha(fecha)
                .fechaInicio(hora)
                .build();
    }

    private Service servicio() {
        return Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte")
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
    }
}
