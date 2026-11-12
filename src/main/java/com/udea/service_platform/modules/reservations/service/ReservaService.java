package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaRescheduleRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.dto.ReservaStatusUpdateRequest;
import com.udea.service_platform.modules.users.dto.ReservaSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Dependency Inversion: controllers depend on this abstraction,
 * not on the concrete implementation.
 */
public interface ReservaService {

    ReservaResponse createReservation(ReservaRequest request, Long clientUserId);

    ReservaResponse cancelReservation(Long reservationId, Long actorUserId);

    ReservaResponse updateStatus(Long reservationId,
                                 ReservaStatusUpdateRequest request,
                                 Long actorUserId);

    ReservaResponse rescheduleReservation(Long reservationId,
                                          ReservaRescheduleRequest request,
                                          Long actorUserId);

    Page<ReservaResponse> getProviderReservations(Long providerId, Pageable pageable);

    Page<ReservaSummaryResponse> getClientReservations(Long clienteId, Pageable pageable);
}
