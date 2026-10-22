package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.modules.services.model.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    List<Bloqueo> findByIdUsuarioProveedorAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
            Long idUsuarioProveedor, LocalDate fecha);

    Optional<Bloqueo> findByIdBloqueoAndIdUsuarioProveedor(Long idBloqueo, Long idUsuarioProveedor);

    List<Bloqueo> findByIdUsuarioProveedorAndFecha(Long idUsuarioProveedor, LocalDate fecha);
}
