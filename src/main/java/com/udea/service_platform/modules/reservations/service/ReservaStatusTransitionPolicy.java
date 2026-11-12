package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * HU-18: guards which operational state changes a provider may apply through
 * {@code PATCH /api/reservations/{id}/status}.
 *
 * <p>The matrix is forward-only and allows skipping intermediate states, so a
 * provider can mark a past reservation as completed without walking through
 * every state. Cancellation is deliberately excluded: it has its own endpoint
 * with its own window rules (HU-19).</p>
 */
@Component
public class ReservaStatusTransitionPolicy {

    private static final Map<EstadoReserva, Set<EstadoReserva>> ALLOWED_TRANSITIONS = Map.of(
            EstadoReserva.ACTIVA, EnumSet.of(
                    EstadoReserva.CONFIRMADA,
                    EstadoReserva.EN_PROCESO,
                    EstadoReserva.COMPLETADA,
                    EstadoReserva.NO_ASISTIO),
            EstadoReserva.CONFIRMADA, EnumSet.of(
                    EstadoReserva.EN_PROCESO,
                    EstadoReserva.COMPLETADA,
                    EstadoReserva.NO_ASISTIO),
            EstadoReserva.EN_PROCESO, EnumSet.of(
                    EstadoReserva.COMPLETADA)
    );

    /**
     * Parses the requested target state. Unknown or missing labels are a
     * client error (HTTP 400), not a state conflict.
     */
    public EstadoReserva parseTarget(String rawEstado) {
        if (rawEstado == null || rawEstado.isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        try {
            return EstadoReserva.fromLabel(rawEstado);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Estado no válido: '" + rawEstado.trim()
                            + "'. Valores permitidos: CONFIRMADA, EN_PROCESO, COMPLETADA, NO_ASISTIO");
        }
    }

    /**
     * Validates the transition from the reservation's current state to the
     * requested target. Any rule violation is a conflict (HTTP 409).
     */
    public void validate(Reserva reserva, EstadoReserva target) {
        EstadoReserva current = resolveCurrentState(reserva);

        if (target == EstadoReserva.CANCELADA) {
            throw new ConflictException(
                    "Usa /api/reservations/{id}/cancel para cancelar una reserva");
        }

        if (target == EstadoReserva.ACTIVA) {
            throw new ConflictException("No se puede volver al estado ACTIVA");
        }

        if (current == target) {
            throw new ConflictException(
                    "La reserva ya está en estado " + target.getLabel());
        }

        if (current.isTerminal()) {
            throw new ConflictException(
                    "No se puede cambiar el estado de una reserva en estado "
                            + current.getLabel());
        }

        Set<EstadoReserva> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new ConflictException(
                    "Transición no permitida: " + current.getLabel()
                            + " -> " + target.getLabel());
        }
    }

    private EstadoReserva resolveCurrentState(Reserva reserva) {
        if (reserva.getIdEstado() == null) {
            throw new ConflictException("La reserva no tiene un estado válido");
        }
        try {
            return EstadoReserva.fromId(reserva.getIdEstado());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("La reserva no tiene un estado válido");
        }
    }
}
