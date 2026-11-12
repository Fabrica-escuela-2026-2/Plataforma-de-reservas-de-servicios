package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaStatusTransitionPolicyTest {

    private final ReservaStatusTransitionPolicy policy = new ReservaStatusTransitionPolicy();

    // ---------- Allowed transitions ----------

    @Test
    void activaAConfirmada_esPermitida() {
        assertAllowed(EstadoReserva.ACTIVA, EstadoReserva.CONFIRMADA);
    }

    @Test
    void activaAEnProceso_esPermitida() {
        assertAllowed(EstadoReserva.ACTIVA, EstadoReserva.EN_PROCESO);
    }

    @Test
    void activaACompletada_esPermitida() {
        assertAllowed(EstadoReserva.ACTIVA, EstadoReserva.COMPLETADA);
    }

    @Test
    void activaANoAsistio_esPermitida() {
        assertAllowed(EstadoReserva.ACTIVA, EstadoReserva.NO_ASISTIO);
    }

    @Test
    void confirmadaAEnProceso_esPermitida() {
        assertAllowed(EstadoReserva.CONFIRMADA, EstadoReserva.EN_PROCESO);
    }

    @Test
    void confirmadaACompletada_esPermitida() {
        assertAllowed(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA);
    }

    @Test
    void confirmadaANoAsistio_esPermitida() {
        assertAllowed(EstadoReserva.CONFIRMADA, EstadoReserva.NO_ASISTIO);
    }

    @Test
    void enProcesoACompletada_esPermitida() {
        assertAllowed(EstadoReserva.EN_PROCESO, EstadoReserva.COMPLETADA);
    }

    // ---------- Forbidden transitions ----------

    @Test
    void desdeCancelada_siempreEsConflicto() {
        assertConflict(EstadoReserva.CANCELADA, EstadoReserva.CONFIRMADA,
                "No se puede cambiar el estado de una reserva en estado CANCELADA");
    }

    @Test
    void desdeCompletada_siempreEsConflicto() {
        assertConflict(EstadoReserva.COMPLETADA, EstadoReserva.EN_PROCESO,
                "No se puede cambiar el estado de una reserva en estado COMPLETADA");
    }

    @Test
    void desdeNoAsistio_siempreEsConflicto() {
        assertConflict(EstadoReserva.NO_ASISTIO, EstadoReserva.COMPLETADA,
                "No se puede cambiar el estado de una reserva en estado NO_ASISTIO");
    }

    @Test
    void enProcesoAConfirmada_esUnSaltoHaciaAtras() {
        assertConflict(EstadoReserva.EN_PROCESO, EstadoReserva.CONFIRMADA,
                "Transición no permitida: EN_PROCESO -> CONFIRMADA");
    }

    @Test
    void enProcesoANoAsistio_noEstaPermitida() {
        assertConflict(EstadoReserva.EN_PROCESO, EstadoReserva.NO_ASISTIO,
                "Transición no permitida: EN_PROCESO -> NO_ASISTIO");
    }

    @Test
    void confirmadaAActiva_esUnSaltoHaciaAtras() {
        assertConflict(EstadoReserva.CONFIRMADA, EstadoReserva.ACTIVA,
                "No se puede volver al estado ACTIVA");
    }

    @Test
    void objetivoCancelada_indicaUsarElEndpointDeCancelacion() {
        assertConflict(EstadoReserva.ACTIVA, EstadoReserva.CANCELADA,
                "Usa /api/reservations/{id}/cancel para cancelar una reserva");
    }

    @Test
    void mismoEstado_esConflicto() {
        assertConflict(EstadoReserva.ACTIVA, EstadoReserva.ACTIVA,
                "No se puede volver al estado ACTIVA");
        assertConflict(EstadoReserva.CONFIRMADA, EstadoReserva.CONFIRMADA,
                "La reserva ya está en estado CONFIRMADA");
    }

    @Test
    void estadoActualNulo_esConflicto() {
        Reserva reserva = reservaEn(null);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validate(reserva, EstadoReserva.CONFIRMADA));

        assertEquals("La reserva no tiene un estado válido", ex.getMessage());
    }

    @Test
    void estadoActualDesconocido_esConflicto() {
        Reserva reserva = reservaEn(99);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validate(reserva, EstadoReserva.CONFIRMADA));

        assertEquals("La reserva no tiene un estado válido", ex.getMessage());
    }

    // ---------- Target parsing ----------

    @Test
    void parseTarget_aceptaEtiquetasValidas() {
        assertEquals(EstadoReserva.CONFIRMADA, policy.parseTarget("CONFIRMADA"));
        assertEquals(EstadoReserva.EN_PROCESO, policy.parseTarget("EN_PROCESO"));
        assertEquals(EstadoReserva.COMPLETADA, policy.parseTarget("COMPLETADA"));
        assertEquals(EstadoReserva.NO_ASISTIO, policy.parseTarget("NO_ASISTIO"));
    }

    @Test
    void parseTarget_normalizaMayusculasYEspacios() {
        assertEquals(EstadoReserva.CONFIRMADA, policy.parseTarget("  confirmada "));
    }

    @Test
    void parseTarget_desconocidoLanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.parseTarget("PAUSADA"));

        assertEquals(
                "Estado no válido: 'PAUSADA'. Valores permitidos: CONFIRMADA, EN_PROCESO, COMPLETADA, NO_ASISTIO",
                ex.getMessage());
    }

    @Test
    void parseTarget_nuloOVacioLanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.parseTarget(null));
        assertEquals("El estado es obligatorio", ex.getMessage());

        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> policy.parseTarget("   "));
        assertEquals("El estado es obligatorio", blank.getMessage());
    }

    // ---------- Helpers ----------

    private void assertAllowed(EstadoReserva current, EstadoReserva target) {
        assertDoesNotThrow(() -> policy.validate(reservaEn(current.getId()), target));
    }

    private void assertConflict(EstadoReserva current, EstadoReserva target, String message) {
        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validate(reservaEn(current.getId()), target));

        assertEquals(message, ex.getMessage());
    }

    private Reserva reservaEn(Integer idEstado) {
        return Reserva.builder()
                .id(100L)
                .idEstado(idEstado)
                .build();
    }
}
