package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import com.udea.service_platform.modules.users.service.ClientDisplayInfo;
import com.udea.service_platform.modules.users.service.ClientLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final Long SERVICE_ID = 10L;
    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ServiceLookupService serviceLookupService;

    @Mock
    private ClientLookupService clientLookupService;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private void stubServicio() {
        when(serviceLookupService.findActiveService(SERVICE_ID)).thenReturn(servicio());
    }

    @Test
    void crearReserva_conFechaYHoraValidas_guardaElInstantCorrecto() {
        stubServicio();
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> {
            Reserva reserva = invocacion.getArgument(0);
            reserva.setId(100L);
            return reserva;
        });
        when(clientLookupService.findClientDisplayInfo(CLIENT_ID))
                .thenReturn(new ClientDisplayInfo(CLIENT_ID, "Cliente", "cliente@test.com"));

        ReservaRequest request = request("2026-10-20", "14:30");

        ReservaResponse response = reservaService.createReservation(request, CLIENT_ID);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        Mockito.verify(reservaRepository).save(captor.capture());

        Instant esperado = Instant.parse("2026-10-20T19:30:00Z");
        assertEquals(esperado, captor.getValue().getHoraInicio());
        assertEquals(LocalTime.of(14, 30), response.getFechaInicio());
        assertEquals(100L, response.getId());
    }

    @Test
    void crearReserva_conFormatoDeFechaInvalido_devuelve400() {
        stubServicio();
        ReservaRequest request = request("20/10/2026", "14:30");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request, CLIENT_ID));

        assertEquals("Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)", ex.getMessage());
    }

    @Test
    void crearReserva_conFormatoDeHoraInvalido_devuelve400() {
        stubServicio();
        ReservaRequest request = request("2026-10-20", "25:99");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request, CLIENT_ID));

        assertEquals("Formato de hora inválido. Use HH:mm (ej. 14:30)", ex.getMessage());
    }

    @Test
    void crearReserva_delPropioProveedor_devuelve400() {
        stubServicio();
        ReservaRequest request = request("2026-10-20", "14:30");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request, PROVIDER_ID));

        assertEquals("No puedes reservar tu propio servicio", ex.getMessage());
        Mockito.verify(reservaRepository, Mockito.never()).save(any(Reserva.class));
    }

    @Test
    void listarReservasDelProveedor_convierteLaHoraLocalYElCliente() {
        Reserva reserva = reservaExistente(2L);
        Page<Reserva> page = new PageImpl<>(List.of(reserva));
        when(reservaRepository.findByProviderIdOrderByFechaInicioDesc(eq(PROVIDER_ID), any()))
                .thenReturn(page);
        when(clientLookupService.findClientDisplayInfoByIds(any()))
                .thenReturn(Map.of(2L, new ClientDisplayInfo(2L, "Cliente", "cliente@test.com")));

        Page<ReservaResponse> result = reservaService.getProviderReservations(
                PROVIDER_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        ReservaResponse response = result.getContent().get(0);
        assertEquals(LocalTime.of(14, 30), response.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 20), response.getFecha());
        assertEquals("Cliente", response.getClienteNombre());
    }

    @Test
    void listarReservasDelProveedor_conClienteDesconocido_muestraUnknown() {
        Reserva reserva = reservaExistente(2L);
        Page<Reserva> page = new PageImpl<>(List.of(reserva));
        when(reservaRepository.findByProviderIdOrderByFechaInicioDesc(eq(PROVIDER_ID), any()))
                .thenReturn(page);
        when(clientLookupService.findClientDisplayInfoByIds(any())).thenReturn(Map.of());

        Page<ReservaResponse> result = reservaService.getProviderReservations(
                PROVIDER_ID, PageRequest.of(0, 10));

        assertEquals("Unknown", result.getContent().get(0).getClienteNombre());
    }

    @Test
    void listarReservasDelCliente_devuelveElResumenConFecha() {
        Reserva reserva = reservaExistente(2L);
        when(reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(eq(CLIENT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(reserva)));
        when(clientLookupService.findClientDisplayInfo(CLIENT_ID))
                .thenReturn(new ClientDisplayInfo(CLIENT_ID, "Cliente", "cliente@test.com"));

        Page<ReservaSummaryResponse> result = reservaService.getClientReservations(
                CLIENT_ID, PageRequest.of(0, 10));

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
