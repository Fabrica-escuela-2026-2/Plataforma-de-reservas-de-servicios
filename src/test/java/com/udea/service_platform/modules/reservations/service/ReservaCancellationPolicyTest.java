package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.model.Service;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaCancellationPolicyTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-20T13:00:00Z"), BOGOTA);
    private static final Instant NOW = Instant.now(CLOCK);

    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;
    private static final Long UNRELATED_ID = 99L;

    private final ReservaCancellationPolicy policy =
            new ReservaCancellationPolicy(CLOCK, 24);

    @Test
    void clienteConMasDe24Horas_puedeCancelar() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, NOW.plus(Duration.ofHours(25)));

        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, CLIENT_ID);

        assertEquals(ReservaCancellationPolicy.CancellationActor.CLIENT, actor);
        assertDoesNotThrow(() -> policy.validateCancellation(reserva, actor));
    }

    @Test
    void clienteConNueveDiasCalendarioDeAnticipacion_puedeCancelar() {
        // Caso reportado: hoy 2026-11-10 y reserva el 2026-11-19 a las 08:00
        // Bogota. Son 9 dias calendario (> 24 horas), por lo que SI se permite.
        Clock clockDelCaso = Clock.fixed(
                LocalDate.of(2026, 11, 10).atTime(8, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        ReservaCancellationPolicy policyDelCaso =
                new ReservaCancellationPolicy(clockDelCaso, 24);

        Instant inicio = LocalDate.of(2026, 11, 19).atTime(8, 0)
                .atZone(BOGOTA).toInstant();
        Reserva reserva = reserva(EstadoReserva.ACTIVA, inicio);

        ReservaCancellationPolicy.CancellationActor actor =
                policyDelCaso.resolveActor(reserva, CLIENT_ID);

        assertDoesNotThrow(() -> policyDelCaso.validateCancellation(reserva, actor));
    }

    @Test
    void clienteExactamente24Horas_puedeCancelar() {
        Reserva reserva = reserva(EstadoReserva.CONFIRMADA, NOW.plus(Duration.ofHours(24)));

        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, CLIENT_ID);

        assertDoesNotThrow(() -> policy.validateCancellation(reserva, actor));
    }

    @Test
    void clienteConMenosDe24Horas_noPuedeCancelar() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, NOW.plus(Duration.ofHours(23)));
        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, CLIENT_ID);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validateCancellation(reserva, actor));

        assertEquals("Solo puedes cancelar con al menos 24 horas de anticipación", ex.getMessage());
    }

    @Test
    void proveedorConMenosDe24Horas_puedeCancelar() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, NOW.plus(Duration.ofHours(1)));

        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, PROVIDER_ID);

        assertEquals(ReservaCancellationPolicy.CancellationActor.PROVIDER, actor);
        assertDoesNotThrow(() -> policy.validateCancellation(reserva, actor));
    }

    @Test
    void proveedorDespuesDelInicio_noPuedeCancelar() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, NOW.minusSeconds(1));
        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, PROVIDER_ID);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validateCancellation(reserva, actor));

        assertEquals("La reserva ya inició y no se puede cancelar", ex.getMessage());
    }

    @Test
    void usuarioNoRelacionado_lanzaAccessDenied() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, NOW.plus(Duration.ofHours(48)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> policy.resolveActor(reserva, UNRELATED_ID));

        assertEquals("No tienes permiso para cancelar esta reserva", ex.getMessage());
    }

    @Test
    void estadosNoCancelables_lanzanConflict() {
        for (EstadoReserva estado : List.of(
                EstadoReserva.CANCELADA,
                EstadoReserva.COMPLETADA,
                EstadoReserva.NO_ASISTIO,
                EstadoReserva.EN_PROCESO)) {
            Reserva reserva = reserva(estado, NOW.plus(Duration.ofHours(48)));
            ReservaCancellationPolicy.CancellationActor actor =
                    policy.resolveActor(reserva, CLIENT_ID);

            assertThrows(ConflictException.class,
                    () -> policy.validateCancellation(reserva, actor),
                    "Estado no cancelable: " + estado);
        }
    }

    @Test
    void reservaSinHoraInicio_permiteCancelar() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA, null);
        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, CLIENT_ID);

        assertDoesNotThrow(() -> policy.validateCancellation(reserva, actor));
    }

    @Test
    void reservaSinEstado_lanzaConflict() {
        Reserva reserva = reserva(null, NOW.plus(Duration.ofHours(48)));
        ReservaCancellationPolicy.CancellationActor actor =
                policy.resolveActor(reserva, CLIENT_ID);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validateCancellation(reserva, actor));

        assertEquals("La reserva no tiene un estado válido", ex.getMessage());
    }

    private Reserva reserva(EstadoReserva estado, Instant horaInicio) {
        return Reserva.builder()
                .id(100L)
                .idEstado(estado != null ? estado.getId() : null)
                .idUsuario(CLIENT_ID)
                .servicio(servicio())
                .horaInicio(horaInicio)
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
    }

    private Service servicio() {
        return Service.builder()
                .id(10L)
                .nombre("Corte")
                .precio(new BigDecimal("25000.00"))
                .duracion(30)
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
    }
}
