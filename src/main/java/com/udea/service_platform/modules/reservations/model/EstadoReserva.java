package com.udea.service_platform.modules.reservations.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Domain representation of the reservation states stored in the
 * {@code Reservas.estado_reserva} column.
 * Single source of truth for DB id <-> UI label mapping.
 */
@Getter
@RequiredArgsConstructor
public enum EstadoReserva {

    ACTIVA(1, "ACTIVA"),
    CANCELADA(2, "CANCELADA"),
    COMPLETADA(3, "COMPLETADA");

    private final Integer id;
    private final String label;

    /**
     * Maps a database state id to its enum constant.
     * Fail-fast: unknown ids throw an explicit exception instead of
     * silently falling back to a default state.
     */
    public static EstadoReserva fromId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Estado de reserva desconocido: null");
        }
        for (EstadoReserva estado : values()) {
            if (estado.id.equals(id)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de reserva desconocido: " + id);
    }
}
