package com.udea.service_platform.modules.services.service;

/**
 * Facade published by the services module for administrative operations
 * that other modules must trigger without injecting ServiceRepository.
 */
public interface ServiceAdministrationService {

    /**
     * Deactivates every active service owned by the given provider.
     * Returns the number of affected rows.
     */
    int deactivateServicesByProvider(Long idProveedor);
}
