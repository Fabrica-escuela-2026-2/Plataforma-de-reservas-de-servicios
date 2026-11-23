package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.resources.service.ResourceReservationGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ResourceReservationGuardImpl implements ResourceReservationGuard {

    private final ReservaRepository reservaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasFutureOccupyingReservations(Long idRecurso, Instant desde) {
        return reservaRepository.existsFutureOccupyingByRecurso(
                idRecurso, EstadoReserva.occupyingStateIds(), desde);
    }
}
