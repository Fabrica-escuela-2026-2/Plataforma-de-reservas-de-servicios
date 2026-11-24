package com.udea.service_platform.modules.services.service;

/**
 * HU-21: puerto estrecho para validar el aforo sin importar reservas desde
 * services. La implementacion vive en reservations y solo recibe primitivos.
 */
public interface AforoChangeGuard {

    void lockResource(Long idRecurso);

    void validatePeak(Long serviceId, Integer duracionServicio, Integer nuevoAforo);
}
