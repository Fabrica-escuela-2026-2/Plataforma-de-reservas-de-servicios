package com.udea.service_platform.modules.reservations.mapper;

import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.model.Service;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaMapperTest {

    @Test
    void toEntity_copiaPrecioYDuracionDelServicio_aValorAcordadoYDuracionAcordada() {
        Service servicio = Service.builder()
                .id(1L)
                .nombre("Corte")
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(true)
                .build();

        Reserva reserva = ReservaMapper.toEntity(servicio, 2L, LocalTime.of(14, 30));

        assertEquals(new BigDecimal("25000.00"), reserva.getValorAcordado());
        assertEquals(30, reserva.getDuracionAcordada());
        assertEquals(servicio, reserva.getServicio());
        assertEquals(2L, reserva.getIdUsuario());
        assertEquals(LocalTime.of(14, 30), reserva.getFechaInicio());
    }

    @Test
    void snapshot_reservaConservaValorAcordadoOriginal_siServicioCambiaPrecio() {
        Service servicio = Service.builder()
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .build();

        Reserva reserva = ReservaMapper.toEntity(servicio, 2L, LocalTime.of(9, 0));

        // Simulamos cambio posterior del servicio
        servicio.setPrecio(new BigDecimal("35000.00"));
        servicio.setDuracion(45);

        // El snapshot de la reserva debe conservar los valores originales
        assertEquals(new BigDecimal("25000.00"), reserva.getValorAcordado());
        assertEquals(30, reserva.getDuracionAcordada());
    }

    @Test
    void toResponse_incluyeValorAcordadoYDuracionAcordada() {
        Reserva reserva = Reserva.builder()
                .id(100L)
                .idEstado(1)
                .idUsuario(2L)
                .fechaInicio(LocalTime.of(14, 30))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();

        ReservaResponse response = ReservaMapper.toResponse(reserva, "Corte", "Cliente", "cliente@test.com");

        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(30, response.getDuracionAcordada());
        assertEquals(100L, response.getId());
        assertEquals("Corte", response.getServicioNombre());
    }
}
