package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.model.Service;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaReschedulePolicyTest {

    private static final Long CLIENT_ID = 2L;
    private static final Long PROVIDER_ID = 5L;

    private final ReservaReschedulePolicy policy = new ReservaReschedulePolicy();

    // ---------- Authorization ----------

    @Test
    void clientePropietario_puedeReprogramar() {
        assertDoesNotThrow(() -> policy.validateActor(reserva(EstadoReserva.ACTIVA), CLIENT_ID));
    }

    @Test
    void proveedorPropietario_puedeReprogramar() {
        assertDoesNotThrow(() -> policy.validateActor(reserva(EstadoReserva.ACTIVA), PROVIDER_ID));
    }

    @Test
    void usuarioAjeno_noPuedeReprogramar() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> policy.validateActor(reserva(EstadoReserva.ACTIVA), 99L));

        assertEquals("No tienes permiso para reprogramar esta reserva", ex.getMessage());
    }

    @Test
    void reservaSinServicioYUsuarioAjeno_noPuedeReprogramar() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        reserva.setServicio(null);

        assertThrows(AccessDeniedException.class,
                () -> policy.validateActor(reserva, PROVIDER_ID));
    }

    // ---------- State eligibility ----------

    @Test
    void activaYConfirmada_sonReprogramables() {
        assertDoesNotThrow(() -> policy.validateState(reserva(EstadoReserva.ACTIVA)));
        assertDoesNotThrow(() -> policy.validateState(reserva(EstadoReserva.CONFIRMADA)));
    }

    @Test
    void enProceso_noEsReprogramable() {
        assertStateConflict(EstadoReserva.EN_PROCESO,
                "La reserva no se puede reprogramar en estado EN_PROCESO");
    }

    @Test
    void cancelada_noEsReprogramable() {
        assertStateConflict(EstadoReserva.CANCELADA,
                "La reserva no se puede reprogramar en estado CANCELADA");
    }

    @Test
    void completada_noEsReprogramable() {
        assertStateConflict(EstadoReserva.COMPLETADA,
                "La reserva no se puede reprogramar en estado COMPLETADA");
    }

    @Test
    void noAsistio_noEsReprogramable() {
        assertStateConflict(EstadoReserva.NO_ASISTIO,
                "La reserva no se puede reprogramar en estado NO_ASISTIO");
    }

    @Test
    void estadoNulo_esConflicto() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        reserva.setIdEstado(null);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validateState(reserva));

        assertEquals("La reserva no tiene un estado válido", ex.getMessage());
    }

    @Test
    void estadoDesconocido_esConflicto() {
        Reserva reserva = reserva(EstadoReserva.ACTIVA);
        reserva.setIdEstado(99);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validateState(reserva));

        assertEquals("La reserva no tiene un estado válido", ex.getMessage());
    }

    // ---------- Parsing ----------

    @Test
    void parseFecha_aceptaFormatoIso() {
        assertEquals(LocalDate.of(2026, 10, 20), policy.parseFecha("2026-10-20"));
    }

    @Test
    void parseFecha_invalidaLanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.parseFecha("20/10/2026"));

        assertEquals("Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)",
                ex.getMessage());
    }

    @Test
    void parseHora_aceptaFormatoHhMm() {
        assertEquals(LocalTime.of(14, 30), policy.parseHora("14:30"));
    }

    @Test
    void parseHora_invalidaLanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> policy.parseHora("25:99"));

        assertEquals("Formato de hora inválido. Use HH:mm (ej. 14:30)", ex.getMessage());
    }

    // ---------- Helpers ----------

    private void assertStateConflict(EstadoReserva estado, String message) {
        ConflictException ex = assertThrows(ConflictException.class,
                () -> policy.validateState(reserva(estado)));

        assertEquals(message, ex.getMessage());
    }

    private Reserva reserva(EstadoReserva estado) {
        return Reserva.builder()
                .id(100L)
                .idEstado(estado.getId())
                .idUsuario(CLIENT_ID)
                .servicio(Service.builder()
                        .id(10L)
                        .nombre("Corte")
                        .idProveedor(PROVIDER_ID)
                        .duracion(60)
                        .build())
                .build();
    }
}
