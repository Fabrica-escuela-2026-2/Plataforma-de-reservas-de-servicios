package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.modules.services.model.Service;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long>, JpaSpecificationExecutor<Service> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Service s SET s.activo = false WHERE s.idProveedor = :idProveedor AND s.activo = true")
    int deactivateServicesByProvider(@Param("idProveedor") Long idProveedor);

    List<Service> findByIdRecursosIn(Collection<Long> idsRecursos);

    /**
     * Loads a service taking a pessimistic write lock on its row (SELECT ... FOR UPDATE).
     * Must run inside a transaction: the lock is held until that transaction ends.
     * Used to serialize concurrent bookings of the same service.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Service s WHERE s.id = :id")
    Optional<Service> findByIdForUpdate(@Param("id") Long id);
}
