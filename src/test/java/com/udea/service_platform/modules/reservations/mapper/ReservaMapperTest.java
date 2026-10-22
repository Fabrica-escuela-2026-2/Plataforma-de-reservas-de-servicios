package com.udea.service_platform.modules.reservations.mapper;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReservaMapperTest {

    private static Instant bogota(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day).atTime(hour, minute)
                .atZone(TimeConfig.ZONA_BOGOTA).toInstant();
    }

    private static Service servicioBase() {
        return Service.builder()
                .id(1L)
                .nombre("Corte")
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(true)
                .build();
    }

    @Test
    void toEntity_copiaPrecioYDuracionDelServicio_aValorAcordadoYDuracionAcordada() {
        Service servicio = servicioBase();
        Instant horaInicio = bogota(2026, 10, 20, 14, 30);

        Reserva reserva = ReservaMapper.toEntity(servicio, 2L, horaInicio);

        assertEquals(new BigDecimal("25000.00"), reserva.getValorAcordado());
        assertEquals(30, reserva.getDuracionAcordada());
        assertEquals(servicio, reserva.getServicio());
        assertEquals(2L, reserva.getIdUsuario());
        assertEquals(horaInicio, reserva.getHoraInicio());
    }

    @Test
    void snapshot_reservaConservaValorAcordadoOriginal_siServicioCambiaPrecio() {
        Service servicio = Service.builder()
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .build();

        Reserva reserva = ReservaMapper.toEntity(servicio, 2L, bogota(2026, 10, 20, 9, 0));

        servicio.setPrecio(new BigDecimal("35000.00"));
        servicio.setDuracion(45);

        assertEquals(new BigDecimal("25000.00"), reserva.getValorAcordado());
        assertEquals(30, reserva.getDuracionAcordada());
    }

    @Test
    void toResponse_convierteHoraInicioALaHoraLocalDeBogota() {
        Reserva reserva = Reserva.builder()
                .id(100L)
                .idEstado(1)
                .idUsuario(2L)
                .horaInicio(bogota(2026, 10, 20, 14, 30))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();

        ReservaResponse response = ReservaMapper.toResponse(reserva, "Corte", "Cliente", "cliente@test.com");

        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(30, response.getDuracionAcordada());
        assertEquals(100L, response.getId());
        assertEquals("Corte", response.getServicioNombre());
        assertEquals(LocalTime.of(14, 30), response.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 20), response.getFecha());
    }

    @Test
    void toResponse_conHoraInicioNula_devuelveFechaYHoraNulas() {
        Reserva reserva = Reserva.builder()
                .id(101L)
                .idEstado(1)
                .horaInicio(null)
                .build();

        ReservaResponse response = ReservaMapper.toResponse(reserva, "Corte", "Cliente", "cliente@test.com");

        assertNull(response.getFechaInicio());
        assertNull(response.getFecha());
    }

    @Test
    void toSummaryResponse_convierteHoraInicioYAgregaFecha() {
        Reserva reserva = Reserva.builder()
                .id(102L)
                .idEstado(1)
                .servicio(Service.builder().nombre("Corte").build())
                .horaInicio(bogota(2026, 10, 20, 8, 15))
                .build();

        ReservaSummaryResponse summary = ReservaMapper.toSummaryResponse(reserva);

        assertEquals(LocalTime.of(8, 15), summary.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 20), summary.getFecha());
    }

    @Test
    void toSummaryResponse_conHoraInicioNula_devuelveFechaYHoraNulas() {
        Reserva reserva = Reserva.builder()
                .id(103L)
                .idEstado(1)
                .servicio(Service.builder().nombre("Corte").build())
                .horaInicio(null)
                .build();

        ReservaSummaryResponse summary = ReservaMapper.toSummaryResponse(reserva);

        assertNull(summary.getFechaInicio());
        assertNull(summary.getFecha());
    }
}
