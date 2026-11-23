package com.udea.service_platform.modules.resources.service;

import java.time.Instant;

/**
 * Dependency inversion: resources asks whether a reservation blocks the
 * deactivation of one of its resources without importing reservations.
 */
public interface ResourceReservationGuard {

    boolean hasFutureOccupyingReservations(Long idRecurso, Instant desde);
}
