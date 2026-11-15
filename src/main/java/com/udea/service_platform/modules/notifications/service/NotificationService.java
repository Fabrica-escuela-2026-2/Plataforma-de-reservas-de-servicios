package com.udea.service_platform.modules.notifications.service;

import com.udea.service_platform.modules.notifications.dto.NotificacionResponse;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Facade published by the notifications module. Other modules send notices
 * through this contract and never touch NotificacionRepository.
 */
public interface NotificationService {

    /**
     * Stores a notice for the user. It joins the caller's transaction, so the
     * notice exists if and only if the business operation that produced it
     * was committed.
     */
    void notifyUser(Long userId, TipoNotificacion tipo, String mensaje);

    /**
     * Notices of the user, newest first.
     */
    Page<NotificacionResponse> getUserNotifications(Long userId, Pageable pageable);
}
