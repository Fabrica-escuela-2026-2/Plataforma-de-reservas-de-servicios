package com.udea.service_platform.modules.services.dto;

import java.time.LocalTime;

/**
 * Franja horaria semanal publicada por la fachada ScheduleLookupService.
 * El dia sigue el estandar ISO (1 = lunes ... 7 = domingo).
 */
public record HorarioFranja(int diaSemana, LocalTime horaInicio, LocalTime horaFin) {
}
