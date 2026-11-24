package com.udea.service_platform.modules.categories.service;

/**
 * Published lookup facade for the categories module.
 */
public interface CategoriaLookupService {

    /**
     * Returns the category with the given name (case-insensitive), creating it
     * when it does not exist yet. Keeping this behavior preserves the existing
     * free-text category contract of the services API.
     */
    CategoriaInfo resolveOrCreate(String nombre);
}
