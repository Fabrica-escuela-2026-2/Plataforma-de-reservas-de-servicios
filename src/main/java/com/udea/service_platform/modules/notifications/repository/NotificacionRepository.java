package com.udea.service_platform.modules.notifications.repository;

import com.udea.service_platform.modules.notifications.model.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByIdUsuarioOrderByFechaCreacionDescIdDesc(Long idUsuario, Pageable pageable);
}
