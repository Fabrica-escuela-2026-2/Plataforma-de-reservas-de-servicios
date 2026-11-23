package com.udea.service_platform.modules.reservations.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationResourceLockTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @Test
    void lockResource_conIdRecurso_tomaElBloqueoAdvisoryDeTransaccion() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("clave", 9907L)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        new ReservationResourceLock(entityManager).lockResource(9907L);

        verify(entityManager).createNativeQuery("SELECT pg_advisory_xact_lock(:clave)");
        verify(query).setParameter("clave", 9907L);
        verify(query).getResultList();
    }

    @Test
    void lockResource_sinIdRecurso_noEjecutaSQL() {
        new ReservationResourceLock(entityManager).lockResource(null);

        verifyNoInteractions(entityManager);
    }
}
