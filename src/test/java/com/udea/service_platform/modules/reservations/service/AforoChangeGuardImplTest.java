package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AforoChangeGuardImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00Z");

    @Mock
    private ReservationResourceLock resourceLock;

    @Mock
    private ReservationSlotPolicy slotPolicy;

    @Mock
    private Clock clock;

    @InjectMocks
    private AforoChangeGuardImpl guard;

    @Test
    void lockResource_delegaEnElBloqueoAdvisory() {
        guard.lockResource(9907L);

        verify(resourceLock).lockResource(9907L);
    }

    @Test
    void lockResource_sinRecurso_noLlamaAlBloqueo() {
        guard.lockResource(null);

        verify(resourceLock).lockResource(null);
    }

    @Test
    void validatePeak_picoDentroDelAforo_noLanza() {
        when(clock.instant()).thenReturn(AHORA);
        when(slotPolicy.peakConcurrentOccupyingReservations(9908L, 45, AHORA)).thenReturn(2);
        when(slotPolicy.normalizarAforo(2)).thenReturn(2);

        guard.validatePeak(9908L, 45, 2);

        verify(slotPolicy).peakConcurrentOccupyingReservations(9908L, 45, AHORA);
    }

    @Test
    void validatePeak_picoSuperaElAforo_lanzaConflict() {
        when(clock.instant()).thenReturn(AHORA);
        when(slotPolicy.peakConcurrentOccupyingReservations(9908L, 45, AHORA)).thenReturn(3);
        when(slotPolicy.normalizarAforo(1)).thenReturn(1);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> guard.validatePeak(9908L, 45, 1));

        assertEquals("Ya existen más reservas que el nuevo aforo", ex.getMessage());
    }

    @Test
    void validatePeak_conAforoNulo_normalizaAUno() {
        when(clock.instant()).thenReturn(AHORA);
        when(slotPolicy.peakConcurrentOccupyingReservations(9908L, 45, AHORA)).thenReturn(1);
        when(slotPolicy.normalizarAforo(null)).thenReturn(1);

        guard.validatePeak(9908L, 45, null);

        verify(slotPolicy).normalizarAforo(null);
    }
}
