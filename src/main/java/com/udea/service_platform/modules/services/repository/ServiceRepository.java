package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.modules.services.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long>, JpaSpecificationExecutor<Service> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Service s SET s.activo = false WHERE s.idProveedor = :idProveedor AND s.activo = true")
    int deactivateServicesByProvider(@Param("idProveedor") Long idProveedor);
}
