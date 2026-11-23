package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.MiReservaResponse;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.users.service.ContactInfo;
import com.udea.service_platform.modules.users.service.ContactLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MisReservasServiceImplTest {

    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;

    /** 2026-10-15 10:00 hora de Bogota (UTC-5). */
    private static final Instant AHORA = Instant.parse("2026-10-15T15:00:00Z");

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ContactLookupService contactLookupService;

    private MisReservasServiceImpl misReservasService;

    @BeforeEach
    void setUp() {
        misReservasService = new MisReservasServiceImpl(
                reservaRepository, contactLookupService, Clock.fixed(AHORA, TimeConfig.ZONA_BOGOTA));
    }

    @Test
    void comoCliente_muestraElContactoDelProveedorYMarcaLasPendientes() {
        Reserva futura = reservaDe(1L, EstadoReserva.ACTIVA, "2026-10-20T19:30:00Z");
        Reserva cancelada = reservaDe(2L, EstadoReserva.CANCELADA, "2026-10-25T19:30:00Z");
        when(reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(eq(CLIENT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(cancelada, futura)));
        when(contactLookupService.findContactsByIds(any())).thenReturn(Map.of(
                PROVIDER_ID, new ContactInfo(PROVIDER_ID, "Pedro Proveedor", "pedro@test.com", "3001234567")));

        MisReservasResponse response = misReservasService.getMyReservationsAsClient(
                CLIENT_ID, PageRequest.of(0, 10));

        assertNull(response.getMensaje());
        assertEquals(2, response.getTotalElements());
        MiReservaResponse primera = response.getContent().get(0);
        assertEquals(EstadoReserva.CANCELADA.getLabel(), primera.getEstado());
        assertFalse(primera.getPendiente());
        MiReservaResponse segunda = response.getContent().get(1);
        assertEquals(EstadoReserva.ACTIVA.getLabel(), segunda.getEstado());
        assertTrue(segunda.getPendiente());
        assertEquals("Corte", segunda.getServicioNombre());
        assertEquals(LocalDate.of(2026, 10, 20), segunda.getFecha());
        assertEquals(LocalTime.of(14, 30), segunda.getFechaInicio());
        assertEquals("PROVEEDOR", segunda.getContacto().getRol());
        assertEquals("Pedro Proveedor", segunda.getContacto().getNombre());
        assertEquals("pedro@test.com", segunda.getContacto().getCorreo());
        assertEquals("3001234567", segunda.getContacto().getTelefono());
    }

    @Test
    void comoProveedor_muestraElContactoDeCadaCliente() {
        Reserva deClienteUno = reservaDe(1L, EstadoReserva.ACTIVA, "2026-10-20T19:30:00Z");
        Reserva deClienteDos = reservaDe(2L, EstadoReserva.COMPLETADA, "2026-10-10T19:30:00Z");
        deClienteDos.setIdUsuario(3L);
        when(reservaRepository.findByProviderIdOrderByFechaInicioDesc(eq(PROVIDER_ID), any()))
                .thenReturn(new PageImpl<>(List.of(deClienteUno, deClienteDos)));
        when(contactLookupService.findContactsByIds(any())).thenReturn(Map.of(
                CLIENT_ID, new ContactInfo(CLIENT_ID, "Ana Uno", "ana@test.com", "3111111111"),
                3L, new ContactInfo(3L, "Beto Dos", "beto@test.com", null)));

        MisReservasResponse response = misReservasService.getMyReservationsAsProvider(
                PROVIDER_ID, PageRequest.of(0, 10));

        assertEquals("Ana Uno", response.getContent().get(0).getContacto().getNombre());
        assertEquals("CLIENTE", response.getContent().get(0).getContacto().getRol());
        assertEquals("Beto Dos", response.getContent().get(1).getContacto().getNombre());
        assertNull(response.getContent().get(1).getContacto().getTelefono());
        assertEquals(EstadoReserva.COMPLETADA.getLabel(), response.getContent().get(1).getEstado());
        assertFalse(response.getContent().get(1).getPendiente());
    }

    @Test
    void sinReservas_devuelveElMensajeDeSinReservas() {
        when(reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(eq(CLIENT_ID), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(contactLookupService.findContactsByIds(any())).thenReturn(Map.of());

        MisReservasResponse response = misReservasService.getMyReservationsAsClient(
                CLIENT_ID, PageRequest.of(0, 10));

        assertEquals("Todavía no tienes reservas", response.getMensaje());
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void conContactoInexistente_igualListaLaReserva() {
        Reserva reserva = reservaDe(1L, EstadoReserva.ACTIVA, "2026-10-20T19:30:00Z");
        when(reservaRepository.findByIdUsuarioOrderByFechaInicioDesc(eq(CLIENT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(reserva)));
        when(contactLookupService.findContactsByIds(any())).thenReturn(Map.of());

        MisReservasResponse response = misReservasService.getMyReservationsAsClient(
                CLIENT_ID, PageRequest.of(0, 10));

        assertEquals(1, response.getContent().size());
        assertEquals("No disponible", response.getContent().get(0).getContacto().getNombre());
        assertNull(response.getContent().get(0).getContacto().getCorreo());
    }

    private Reserva reservaDe(Long id, EstadoReserva estado, String horaInicioUtc) {
        Service servicio = Service.builder()
                .id(10L)
                .nombre("Corte")
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
        return Reserva.builder()
                .id(id)
                .idEstado(estado.getId())
                .idUsuario(CLIENT_ID)
                .servicio(servicio)
                .horaInicio(Instant.parse(horaInicioUtc))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
    }
}
