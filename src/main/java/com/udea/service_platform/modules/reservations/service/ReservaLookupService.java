package com.udea.service_platform.modules.reservations.service;

/**
 * Fachada publicada por el módulo reservations para consultas de reservas
 * usadas por otros módulos sin inyectar repositorios cruzados.
 */
public interface ReservaLookupService {

    boolean hasActiveReservations(Long userId);
}
