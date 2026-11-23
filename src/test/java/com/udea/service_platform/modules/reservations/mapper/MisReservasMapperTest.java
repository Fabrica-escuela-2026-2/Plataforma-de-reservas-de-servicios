package com.udea.service_platform.modules.reservations.mapper;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.ContactoResponse;
import com.udea.service_platform.modules.reservations.dto.MiReservaResponse;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.users.service.ContactInfo;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MisReservasMapperTest {

    private static Instant bogota(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day).atTime(hour, minute)
                .atZone(TimeConfig.ZONA_BOGOTA).toInstant();
    }

    private static Reserva reservaConEstadoYHora(int estado, Instant horaInicio) {
        return Reserva.builder()
                .id(1L)
                .idEstado(estado)
                .servicio(Service.builder().nombre("Corte").build())
                .horaInicio(horaInicio)
                .build();
    }

    @Test
    void isPendiente_confirmadaYFutura_esTrue() {
        Instant ahora = bogota(2026, 10, 15, 10, 0);

        assertTrue(MisReservasMapper.isPendiente(reservaConEstadoYHora(1, bogota(2026, 10, 15, 10, 30)), ahora));
    }

    @Test
    void isPendiente_confirmadaEnElMinutoActual_esTrue() {
        Instant ahora = bogota(2026, 10, 15, 10, 0).plusSeconds(40);

        assertTrue(MisReservasMapper.isPendiente(reservaConEstadoYHora(1, bogota(2026, 10, 15, 10, 0)), ahora));
    }

    @Test
    void isPendiente_confirmadaPeroYaPasada_esFalse() {
        Instant ahora = bogota(2026, 10, 15, 10, 0);

        assertFalse(MisReservasMapper.isPendiente(reservaConEstadoYHora(1, bogota(2026, 10, 15, 9, 59)), ahora));
    }

    @Test
    void isPendiente_canceladaOCompletadaFuturas_esFalse() {
        Instant ahora = bogota(2026, 10, 15, 10, 0);
        Instant futura = bogota(2026, 10, 20, 10, 0);

        assertFalse(MisReservasMapper.isPendiente(reservaConEstadoYHora(2, futura), ahora));
        assertFalse(MisReservasMapper.isPendiente(reservaConEstadoYHora(3, futura), ahora));
    }

    @Test
    void isPendiente_sinHoraDeInicio_esFalse() {
        assertFalse(MisReservasMapper.isPendiente(reservaConEstadoYHora(1, null), bogota(2026, 10, 15, 10, 0)));
    }

    @Test
    void toMiReservaResponse_copiaLosDatosDeLaReservaYElContacto() {
        Reserva reserva = Reserva.builder()
                .id(9L)
                .idEstado(1)
                .servicio(Service.builder().nombre("Corte").build())
                .horaInicio(bogota(2026, 10, 20, 14, 30))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
        ContactoResponse contacto = MisReservasMapper.toContacto("PROVEEDOR",
                new ContactInfo(5L, "Pedro Proveedor", "pedro@test.com", "300"));

        MiReservaResponse response = MisReservasMapper.toMiReservaResponse(reserva, contacto, bogota(2026, 10, 15, 10, 0));

        assertEquals(9L, response.getId());
        assertEquals("Corte", response.getServicioNombre());
        assertEquals(LocalDate.of(2026, 10, 20), response.getFecha());
        assertEquals(LocalTime.of(14, 30), response.getFechaInicio());
        assertEquals(30, response.getDuracionAcordada());
        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(EstadoReserva.ACTIVA.getLabel(), response.getEstado());
        assertTrue(response.getPendiente());
        assertEquals("pedro@test.com", response.getContacto().getCorreo());
    }

    @Test
    void toMiReservaResponse_conHoraNula_devuelveFechaYHoraNulasYNoPendiente() {
        Reserva reserva = reservaConEstadoYHora(1, null);

        MiReservaResponse response = MisReservasMapper.toMiReservaResponse(
                reserva, MisReservasMapper.toContacto("CLIENTE", null), bogota(2026, 10, 15, 10, 0));

        assertNull(response.getFecha());
        assertNull(response.getFechaInicio());
        assertFalse(response.getPendiente());
        assertEquals("No disponible", response.getContacto().getNombre());
    }

    @Test
    void toMisReservasResponse_conReservas_noLlevaMensaje() {
        MiReservaResponse fila = MiReservaResponse.builder().id(1L).build();

        MisReservasResponse response = MisReservasMapper.toMisReservasResponse(
                new PageImpl<>(List.of(fila), PageRequest.of(0, 10), 1));

        assertNull(response.getMensaje());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getNumber());
        assertEquals(10, response.getSize());
    }

    @Test
    void toMisReservasResponse_sinReservas_llevaElMensaje() {
        MisReservasResponse response = MisReservasMapper.toMisReservasResponse(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        assertEquals("Todavía no tienes reservas", response.getMensaje());
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void toMisReservasResponse_paginaFueraDeRangoConReservas_noLlevaMensaje() {
        MisReservasResponse response = MisReservasMapper.toMisReservasResponse(
                new PageImpl<>(List.of(), PageRequest.of(5, 10), 3));

        assertNull(response.getMensaje());
        assertTrue(response.getContent().isEmpty());
    }
}
