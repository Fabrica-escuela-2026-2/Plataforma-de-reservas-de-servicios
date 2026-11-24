package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;

import java.time.LocalDate;
import java.util.List;

public record SlotContext(Long serviceId,
                          Long idRecurso,
                          Integer aforo,
                          Long providerId,
                          LocalDate fecha,
                          int duracionServicio,
                          List<HorarioFranja> schedule,
                          List<BloqueoFranja> blocks,
                          List<Reserva> sameServiceReservations,
                          List<Reserva> otherServiceReservations,
                          Long excludeReservationId) {
}
