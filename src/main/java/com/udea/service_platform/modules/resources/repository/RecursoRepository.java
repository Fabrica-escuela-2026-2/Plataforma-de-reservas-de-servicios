package com.udea.service_platform.modules.resources.repository;

import com.udea.service_platform.modules.resources.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    List<Recurso> findByIdProveedorOrderByNombreAsc(Long idProveedor);

    List<Recurso> findAllByOrderByNombreAsc();

    boolean existsByIdProveedorAndNombreIgnoreCase(Long idProveedor, String nombre);
}
