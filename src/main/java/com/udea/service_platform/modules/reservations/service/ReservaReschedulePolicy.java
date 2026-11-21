package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * HU-17: authorization and eligibility rules for rescheduling a reservation.
 * Kept separate from the service so they can be unit-tested without mocks.
 */
@Component
public class ReservaReschedulePolicy {

    /**
     * Only the client owner or the provider owner of the reservation's service
     * may reschedule it.
     */
    public void validateActor(Reserva reserva, Long actorUserId) {
        boolean esCliente = Objects.equals(reserva.getIdUsuario(), actorUserId);
        boolean esProveedor = reserva.getServicio() != null
                && Objects.equals(reserva.getServicio().getIdProveedor(), actorUserId);

        if (!esCliente && !esProveedor) {
            throw new AccessDeniedException(
                    "No tienes permiso para reprogramar esta reserva");
        }
    }

    /**
     * Only ACTIVA and CONFIRMADA reservations can be rescheduled. A corrupt or
     * missing state is treated as a conflict, not a client error.
     */
    public void validateState(Reserva reserva) {
        if (reserva.getIdEstado() == null) {
            throw new ConflictException("La reserva no tiene un estado válido");
        }

        EstadoReserva estado;
        try {
            estado = EstadoReserva.fromId(reserva.getIdEstado());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("La reserva no tiene un estado válido");
        }

        if (!estado.isReschedulable()) {
            throw new ConflictException(
                    "La reserva no se puede reprogramar en estado " + estado.getLabel());
        }
    }

    public LocalDate parseFecha(String rawFecha) {
        try {
            return LocalDate.parse(rawFecha);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)");
        }
    }

    public LocalTime parseHora(String rawHora) {
        try {
            return LocalTime.parse(rawHora);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Formato de hora inválido. Use HH:mm (ej. 14:30)");
        }
    }
}
