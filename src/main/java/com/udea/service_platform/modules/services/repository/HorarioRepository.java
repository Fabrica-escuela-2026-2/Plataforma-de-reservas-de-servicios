package com.udea.service_platform.modules.services.repository;

import com.udea.service_platform.modules.services.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    @Query("SELECT h FROM Horario h WHERE h.idServicio = :idServicio ORDER BY h.idDiaSemana, h.horaInicio")
    List<Horario> findByIdServicio(@Param("idServicio") Integer idServicio);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Horario h WHERE h.idServicio = :idServicio")
    void deleteByIdServicio(@Param("idServicio") Integer idServicio);
}
