package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * HU-19: decides who may cancel a reservation and whether the cancellation
 * window is still open. Kept separate from the service so the rules can be
 * unit-tested with a fixed clock.
 */
@Slf4j
@Component
public class ReservaCancellationPolicy {

    public enum CancellationActor {
        CLIENT,
        PROVIDER
    }

    private final Clock clock;
    private final long windowHours;

    public ReservaCancellationPolicy(
            Clock clock,
            @Value("${app.reservations.cancellation-window-hours:24}") long windowHours) {
        this.clock = clock;
        this.windowHours = windowHours;
    }

    /**
     * Resolves the actor's relationship with the reservation. Clients are
     * checked first because the reservation owner is the most specific role.
     */
    public CancellationActor resolveActor(Reserva reserva, Long actorUserId) {
        if (Objects.equals(reserva.getIdUsuario(), actorUserId)) {
            return CancellationActor.CLIENT;
        }

        if (reserva.getServicio() != null
                && Objects.equals(reserva.getServicio().getIdProveedor(), actorUserId)) {
            return CancellationActor.PROVIDER;
        }

        throw new AccessDeniedException("No tienes permiso para cancelar esta reserva");
    }

    /**
     * Validates the reservation state and the cancellation window. Clients
     * need at least {@code windowHours} of anticipation; providers may cancel
     * at any time before the reservation starts.
     */
    public void validateCancellation(Reserva reserva, CancellationActor actor) {
        if (reserva.getIdEstado() == null) {
            throw new ConflictException("La reserva no tiene un estado válido");
        }

        EstadoReserva estado = EstadoReserva.fromId(reserva.getIdEstado());
        if (!estado.isCancellable()) {
            throw new ConflictException(
                    "La reserva no se puede cancelar en estado " + estado.getLabel());
        }

        // Legacy rows may not have a start time. There is no window to enforce.
        if (reserva.getHoraInicio() == null) {
            return;
        }

        Instant now = Instant.now(clock);

        if (!reserva.getHoraInicio().isAfter(now)) {
            log.info("HU-19 cancelacion rechazada: reserva={} actor={} ya inicio "
                            + "(ahora={} horaInicio={})",
                    reserva.getId(), actor, now, reserva.getHoraInicio());
            throw new ConflictException("La reserva ya inició y no se puede cancelar");
        }

        if (actor == CancellationActor.CLIENT) {
            Instant limite = now.plus(Duration.ofHours(windowHours));
            Duration anticipacion = Duration.between(now, reserva.getHoraInicio());
            long diasCalendario = ChronoUnit.DAYS.between(
                    now.atZone(TimeConfig.ZONA_BOGOTA).toLocalDate(),
                    reserva.getHoraInicio().atZone(TimeConfig.ZONA_BOGOTA).toLocalDate());
            boolean permitida = !reserva.getHoraInicio().isBefore(limite);

            // HU-19: la ventana se expresa en horas, no en dias calendario.
            // Ej.: hoy 2026-11-10 y reserva 2026-11-19 => 9 dias calendario
            // (216 h aproximadamente) > 24 h, por lo que SI se permite.
            log.info("HU-19 ventana de cancelacion: reserva={} actor={} ahora={} horaInicio={} "
                            + "anticipacion={}h ({} dias calendario) ventanaRequerida={}h "
                            + "limite={} permitida={}",
                    reserva.getId(), actor, now, reserva.getHoraInicio(),
                    anticipacion.toHours(), diasCalendario, windowHours, limite, permitida);

            if (!permitida) {
                throw new ConflictException(
                        "Solo puedes cancelar con al menos " + windowHours + " horas de anticipación");
            }
        } else {
            log.info("HU-19 cancelacion de proveedor permitida: reserva={} actor={} ahora={} "
                            + "horaInicio={} (sin ventana minima, solo antes del inicio)",
                    reserva.getId(), actor, now, reserva.getHoraInicio());
        }
    }
}
