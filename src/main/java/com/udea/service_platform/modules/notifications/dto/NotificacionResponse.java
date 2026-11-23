package com.udea.service_platform.modules.notifications.dto;

import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionResponse {

    private Long id;
    private TipoNotificacion tipo;
    private String mensaje;
    private LocalDateTime fecha;
    private Boolean leida;
}
