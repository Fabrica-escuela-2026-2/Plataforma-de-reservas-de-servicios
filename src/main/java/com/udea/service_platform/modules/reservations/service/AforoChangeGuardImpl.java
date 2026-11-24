package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.services.service.AforoChangeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class AforoChangeGuardImpl implements AforoChangeGuard {

    static final String MSG_PICO_SUPERA_AFORO = "Ya existen más reservas que el nuevo aforo";

    private final ReservationResourceLock resourceLock;
    private final ReservationSlotPolicy slotPolicy;
    private final Clock clock;

    @Override
    public void lockResource(Long idRecurso) {
        resourceLock.lockResource(idRecurso);
    }

    @Override
    public void validatePeak(Long serviceId, Integer duracionServicio, Integer nuevoAforo) {
        int pico = slotPolicy.peakConcurrentOccupyingReservations(
                serviceId, duracionServicio, clock.instant());
        if (pico > slotPolicy.normalizarAforo(nuevoAforo)) {
            throw new ConflictException(MSG_PICO_SUPERA_AFORO);
        }
    }
}
