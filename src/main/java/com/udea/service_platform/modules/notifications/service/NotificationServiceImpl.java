package com.udea.service_platform.modules.notifications.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.notifications.dto.NotificacionResponse;
import com.udea.service_platform.modules.notifications.model.Notificacion;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificacionRepository notificacionRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void notifyUser(Long userId, TipoNotificacion tipo, String mensaje) {
        Notificacion notificacion = Notificacion.builder()
                .idUsuario(userId)
                .tipo(tipo)
                .mensaje(mensaje)
                .fechaCreacion(clock.instant())
                .leida(false)
                .build();
        notificacionRepository.save(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificacionResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notificacionRepository.findByIdUsuarioOrderByFechaCreacionDescIdDesc(userId, pageable)
                .map(this::toResponse);
    }

    private NotificacionResponse toResponse(Notificacion notificacion) {
        return NotificacionResponse.builder()
                .id(notificacion.getId())
                .tipo(notificacion.getTipo())
                .mensaje(notificacion.getMensaje())
                .fecha(LocalDateTime.ofInstant(notificacion.getFechaCreacion(), TimeConfig.ZONA_BOGOTA))
                .leida(notificacion.getLeida())
                .build();
    }
}
