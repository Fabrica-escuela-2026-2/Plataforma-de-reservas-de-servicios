package com.udea.service_platform.modules.categories.service;

/**
 * Read-only view of a category published to other modules.
 * Prevents leaking the JPA entity across domain boundaries.
 */
public record CategoriaInfo(Long id, String nombre) {
}
