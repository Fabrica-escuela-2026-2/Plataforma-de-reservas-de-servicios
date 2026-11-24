package com.udea.service_platform.modules.reservations.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU-20/HU-21: bloqueo advisory por recurso. Se toma siempre antes de cualquier
 * FOR UPDATE y se libera al terminar la transaccion. La clave es el propio
 * id_recurso, estable para todos los servicios que comparten el recurso.
 */
@Component
@RequiredArgsConstructor
public class ReservationResourceLock {

    private static final String SQL_LOCK = "SELECT pg_advisory_xact_lock(:clave)";

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockResource(Long idRecurso) {
        if (idRecurso == null) {
            return;
        }
        entityManager.createNativeQuery(SQL_LOCK)
                .setParameter("clave", idRecurso)
                .getResultList();
    }
}
