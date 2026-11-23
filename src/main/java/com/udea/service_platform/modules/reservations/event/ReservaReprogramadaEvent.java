package com.udea.service_platform.modules.reservations.event;

import java.time.Instant;

/**
 * HU-17: domain event published when a reservation's start time changes.
 * Consumed after the transaction commits so listeners never observe a
 * reschedule that was rolled back.
 */
public record ReservaReprogramadaEvent(
        Long reservaId,
        Long actorId,
        Long clienteId,
        Long proveedorId,
        Instant horaAnterior,
        Instant horaNueva,
        Instant timestamp) {
}
