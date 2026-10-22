package com.udea.service_platform.modules.services.dto;

import java.time.LocalTime;

/**
 * Bloqueo de agenda publicado por la fachada ScheduleLookupService.
 * horaInicio/horaFin nulas significan bloqueo de dia completo.
 */
public record BloqueoFranja(LocalTime horaInicio, LocalTime horaFin) {
}
