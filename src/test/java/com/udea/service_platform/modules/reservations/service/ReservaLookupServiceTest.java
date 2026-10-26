package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaLookupServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReservaLookupServiceImpl reservaLookupService;

    @Test
    void hasActiveReservations_onlyClient_returnsTrue() {
        when(reservaRepository.existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId()))
                .thenReturn(true);

        boolean result = reservaLookupService.hasActiveReservations(USER_ID);

        assertTrue(result);
        verify(reservaRepository).existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId());
        verify(reservaRepository, never()).existsActiveByProviderId(any(), any());
    }

    @Test
    void hasActiveReservations_onlyProvider_returnsTrue() {
        when(reservaRepository.existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId()))
                .thenReturn(false);
        when(reservaRepository.existsActiveByProviderId(USER_ID, EstadoReserva.ACTIVA.getId()))
                .thenReturn(true);

        boolean result = reservaLookupService.hasActiveReservations(USER_ID);

        assertTrue(result);
        verify(reservaRepository).existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId());
        verify(reservaRepository).existsActiveByProviderId(USER_ID, EstadoReserva.ACTIVA.getId());
    }

    @Test
    void hasActiveReservations_none_returnsFalse() {
        when(reservaRepository.existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId()))
                .thenReturn(false);
        when(reservaRepository.existsActiveByProviderId(USER_ID, EstadoReserva.ACTIVA.getId()))
                .thenReturn(false);

        boolean result = reservaLookupService.hasActiveReservations(USER_ID);

        assertFalse(result);
        verify(reservaRepository).existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId());
        verify(reservaRepository).existsActiveByProviderId(USER_ID, EstadoReserva.ACTIVA.getId());
    }

    @Test
    void hasActiveReservations_both_returnsTrue() {
        when(reservaRepository.existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId()))
                .thenReturn(true);

        boolean result = reservaLookupService.hasActiveReservations(USER_ID);

        assertTrue(result);
        verify(reservaRepository).existsByIdUsuarioAndIdEstado(USER_ID, EstadoReserva.ACTIVA.getId());
        verify(reservaRepository, never()).existsActiveByProviderId(any(), any());
    }

    @Test
    void hasActiveReservations_nullUserId_returnsFalse() {
        boolean result = reservaLookupService.hasActiveReservations(null);

        assertFalse(result);
        verifyNoInteractions(reservaRepository);
    }
}
