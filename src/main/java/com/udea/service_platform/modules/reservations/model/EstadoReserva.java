package com.udea.service_platform.modules.reservations.model;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;

/**
 * Domain representation of the reservation states stored in the
 * {@code Reservas.estado_reserva} column.
 * Single source of truth for DB id <-> UI label mapping and for the
 * business rules that depend on a state (schedule occupancy, cancellation
 * eligibility and terminal states).
 */
@RequiredArgsConstructor
public enum EstadoReserva {

    ACTIVA(1, "ACTIVA", true, true, false),
    CANCELADA(2, "CANCELADA", false, false, true),
    COMPLETADA(3, "COMPLETADA", false, false, true),
    CONFIRMADA(4, "CONFIRMADA", true, true, false),
    EN_PROCESO(5, "EN_PROCESO", true, false, false),
    NO_ASISTIO(6, "NO_ASISTIO", false, false, true);

    private final Integer id;
    private final String label;
    private final boolean occupiesSchedule;
    private final boolean cancellable;
    private final boolean terminal;

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    /**
     * States that keep a time slot busy in the availability calculation.
     */
    public static List<Integer> occupyingStateIds() {
        return List.of(ACTIVA.id, CONFIRMADA.id, EN_PROCESO.id);
    }

    public boolean occupiesSchedule() {
        return occupiesSchedule;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public boolean isTerminal() {
        return terminal;
    }

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

    /**
     * Maps a UI/API label to its enum constant. Labels are case-insensitive
     * and surrounding whitespace is ignored.
     * Fail-fast: unknown labels throw an explicit exception.
     */
    public static EstadoReserva fromLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Estado de reserva desconocido: null");
        }
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        for (EstadoReserva estado : values()) {
            if (estado.label.equals(normalized)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de reserva desconocido: " + label);
    }
}
