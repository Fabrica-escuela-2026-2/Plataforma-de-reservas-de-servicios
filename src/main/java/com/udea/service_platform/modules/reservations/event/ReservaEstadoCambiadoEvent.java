package com.udea.service_platform.modules.reservations.event;

import com.udea.service_platform.modules.reservations.model.EstadoReserva;

import java.time.Instant;

/**
 * Domain event published whenever a reservation changes state. The event is
 * consumed after the transaction commits so listeners never observe a state
 * change that was rolled back.
 */
public record ReservaEstadoCambiadoEvent(
        Long reservaId,
        Long actorId,
        EstadoReserva estadoAnterior,
        EstadoReserva estadoNuevo,
        Instant timestamp) {
}
