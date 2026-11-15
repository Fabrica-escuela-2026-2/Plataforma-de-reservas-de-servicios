package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import org.springframework.data.domain.Pageable;

/**
 * HU-16: "Mis reservas" for the authenticated user.
 */
public interface MisReservasService {

    /**
     * Reservations made by the client, with the contact data of each provider.
     */
    MisReservasResponse getMyReservationsAsClient(Long clientUserId, Pageable pageable);

    /**
     * Reservations of the provider's services, with the contact data of each client.
     */
    MisReservasResponse getMyReservationsAsProvider(Long providerUserId, Pageable pageable);
}
