package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservaLookupServiceImpl implements ReservaLookupService {

    private final ReservaRepository reservaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveReservations(Long userId) {
        if (userId == null) {
            return false;
        }

        boolean hasAsClient = reservaRepository.existsByIdUsuarioAndIdEstado(userId,
                EstadoReserva.ACTIVA.getId());
        if (hasAsClient) {
            return true;
        }

        boolean hasAsProvider = reservaRepository.existsActiveByProviderId(userId,
                EstadoReserva.ACTIVA.getId());
        return hasAsProvider;
    }
}
