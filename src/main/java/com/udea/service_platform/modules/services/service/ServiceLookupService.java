package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.model.Service;

/**
 * Facade published by the services module.
 * Other modules resolve services through this contract
 * instead of injecting ServiceRepository directly.
 */
public interface ServiceLookupService {

    /**
     * Returns the service when it exists and is active,
     * fail-fast otherwise.
     */
    Service findActiveService(Long serviceId);

    /**
     * Same as {@link #findActiveService(Long)} but takes a pessimistic write
     * lock on the service row, so concurrent bookings of the same service run
     * one after another. Requires an active transaction (the lock lives until
     * it ends). Fails with the generic message "El servicio no está disponible"
     * when the service does not exist or is inactive.
     */
    Service lockActiveService(Long serviceId);
}
