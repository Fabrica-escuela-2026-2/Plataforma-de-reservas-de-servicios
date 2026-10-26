package com.udea.service_platform.modules.reservations.repository;

import com.udea.service_platform.modules.reservations.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.idUsuario = :idUsuario "
            + "ORDER BY r.horaInicio DESC NULLS LAST")
    Page<Reserva> findByIdUsuarioOrderByFechaInicioDesc(@Param("idUsuario") Long idUsuario, Pageable pageable);

    @Query("SELECT r FROM Reserva r JOIN r.servicio s WHERE s.idProveedor = :providerId "
            + "ORDER BY r.horaInicio DESC NULLS LAST")
    Page<Reserva> findByProviderIdOrderByFechaInicioDesc(@Param("providerId") Long providerId, Pageable pageable);

    @Query("SELECT r FROM Reserva r WHERE r.servicio.id = :serviceId "
            + "AND r.idEstado = :estadoId "
            + "AND r.horaInicio >= :inicio AND r.horaInicio < :fin")
    List<Reserva> findActiveReservationsInRange(@Param("serviceId") Long serviceId,
                                                @Param("estadoId") Integer estadoId,
                                                @Param("inicio") Instant inicio,
                                                @Param("fin") Instant fin);
    boolean existsByIdUsuarioAndIdEstado(Long idUsuario, Integer idEstado);

    @Query("SELECT COUNT(r)>0 FROM Reserva r JOIN r.servicio s WHERE s.idProveedor = :providerId AND r.idEstado = :estadoId")
    boolean existsActiveByProviderId(@Param("providerId") Long providerId, @Param("estadoId") Integer estadoId);
}
