package com.udea.service_platform.modules.reservations.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoReservaTest {

    @Test
    void fromId_mapeaTodosLosEstados() {
        assertEquals(EstadoReserva.ACTIVA, EstadoReserva.fromId(1));
        assertEquals(EstadoReserva.CANCELADA, EstadoReserva.fromId(2));
        assertEquals(EstadoReserva.COMPLETADA, EstadoReserva.fromId(3));
        assertEquals(EstadoReserva.CONFIRMADA, EstadoReserva.fromId(4));
        assertEquals(EstadoReserva.EN_PROCESO, EstadoReserva.fromId(5));
        assertEquals(EstadoReserva.NO_ASISTIO, EstadoReserva.fromId(6));
    }

    @Test
    void fromId_desconocidoLanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EstadoReserva.fromId(99));

        assertEquals("Estado de reserva desconocido: 99", ex.getMessage());
    }

    @Test
    void fromId_nullLanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EstadoReserva.fromId(null));

        assertEquals("Estado de reserva desconocido: null", ex.getMessage());
    }

    @Test
    void occupyingStateIds_contieneSoloEstadosQueBloqueanAgenda() {
        assertEquals(
                List.of(1, 4, 5),
                EstadoReserva.occupyingStateIds());
    }

    @Test
    void occupiesSchedule_soloActivaConfirmadaYEnProceso() {
        assertTrue(EstadoReserva.ACTIVA.occupiesSchedule());
        assertTrue(EstadoReserva.CONFIRMADA.occupiesSchedule());
        assertTrue(EstadoReserva.EN_PROCESO.occupiesSchedule());

        assertFalse(EstadoReserva.CANCELADA.occupiesSchedule());
        assertFalse(EstadoReserva.COMPLETADA.occupiesSchedule());
        assertFalse(EstadoReserva.NO_ASISTIO.occupiesSchedule());
    }

    @Test
    void isCancellable_soloActivaYConfirmada() {
        assertTrue(EstadoReserva.ACTIVA.isCancellable());
        assertTrue(EstadoReserva.CONFIRMADA.isCancellable());

        assertFalse(EstadoReserva.CANCELADA.isCancellable());
        assertFalse(EstadoReserva.COMPLETADA.isCancellable());
        assertFalse(EstadoReserva.EN_PROCESO.isCancellable());
        assertFalse(EstadoReserva.NO_ASISTIO.isCancellable());
    }

    @Test
    void isTerminal_soloCanceladaCompletadaYNoAsistio() {
        assertTrue(EstadoReserva.CANCELADA.isTerminal());
        assertTrue(EstadoReserva.COMPLETADA.isTerminal());
        assertTrue(EstadoReserva.NO_ASISTIO.isTerminal());

        assertFalse(EstadoReserva.ACTIVA.isTerminal());
        assertFalse(EstadoReserva.CONFIRMADA.isTerminal());
        assertFalse(EstadoReserva.EN_PROCESO.isTerminal());
    }
}
