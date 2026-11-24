package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.EstadoSlot;

public record SlotDecision(EstadoSlot estado,
                           SlotReason reason,
                           int cuposTotales,
                           int cuposDisponibles,
                           String mensaje) {
}
