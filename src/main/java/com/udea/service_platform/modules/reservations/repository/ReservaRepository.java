package com.udea.service_platform.modules.reservations.repository;

import com.udea.service_platform.modules.reservations.model.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("SELECT r FROM Reserva r WHERE r.idUsuario = :idUsuario "
            + "ORDER BY r.horaInicio DESC NULLS LAST")
    Page<Reserva> findByIdUsuarioOrderByFechaInicioDesc(@Param("idUsuario") Long idUsuario, Pageable pageable);

    @Query("SELECT r FROM Reserva r JOIN r.servicio s WHERE s.idProveedor = :providerId "
            + "ORDER BY r.horaInicio DESC NULLS LAST")
    Page<Reserva> findByProviderIdOrderByFechaInicioDesc(@Param("providerId") Long providerId, Pageable pageable);

    /**
     * Locks the reservation row for the duration of the transaction so that
     * two concurrent state changes cannot both read the same previous state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM Reserva r WHERE r.servicio.id = :serviceId "
            + "AND r.idEstado IN :estadoIds "
            + "AND r.horaInicio >= :inicio AND r.horaInicio < :fin")
    List<Reserva> findOccupyingReservationsInRange(@Param("serviceId") Long serviceId,
                                                   @Param("estadoIds") Collection<Integer> estadoIds,
                                                   @Param("inicio") Instant inicio,
                                                   @Param("fin") Instant fin);

    boolean existsByIdUsuarioAndIdEstadoIn(Long idUsuario, Collection<Integer> estadoIds);

    @Query("SELECT COUNT(r)>0 FROM Reserva r JOIN r.servicio s "
            + "WHERE s.idProveedor = :providerId AND r.idEstado IN :estadoIds")
    boolean existsByProviderIdAndIdEstadoIn(@Param("providerId") Long providerId,
                                            @Param("estadoIds") Collection<Integer> estadoIds);
}
