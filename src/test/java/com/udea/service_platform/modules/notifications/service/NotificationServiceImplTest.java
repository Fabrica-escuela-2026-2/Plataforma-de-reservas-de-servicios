package com.udea.service_platform.modules.notifications.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.notifications.dto.NotificacionResponse;
import com.udea.service_platform.modules.notifications.model.Notificacion;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.repository.NotificacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T15:00:00Z");

    @Mock
    private NotificacionRepository notificacionRepository;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificacionRepository, Clock.fixed(AHORA, TimeConfig.ZONA_BOGOTA));
    }

    @Test
    void notifyUser_guardaLaNotificacionSinLeerConLaFechaActual() {
        notificationService.notifyUser(2L, TipoNotificacion.RESERVA_CONFIRMADA, "Reserva confirmada");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());

        Notificacion guardada = captor.getValue();
        assertEquals(2L, guardada.getIdUsuario());
        assertEquals(TipoNotificacion.RESERVA_CONFIRMADA, guardada.getTipo());
        assertEquals("Reserva confirmada", guardada.getMensaje());
        assertEquals(AHORA, guardada.getFechaCreacion());
        assertFalse(guardada.getLeida());
    }

    @Test
    void getUserNotifications_convierteLaFechaAHoraDeBogota() {
        Notificacion notificacion = Notificacion.builder()
                .id(7L)
                .idUsuario(2L)
                .tipo(TipoNotificacion.NUEVA_RESERVA)
                .mensaje("Nueva reserva")
                .fechaCreacion(AHORA)
                .leida(false)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        when(notificacionRepository.findByIdUsuarioOrderByFechaCreacionDescIdDesc(2L, pageable))
                .thenReturn(new PageImpl<>(List.of(notificacion)));

        Page<NotificacionResponse> result = notificationService.getUserNotifications(2L, pageable);

        NotificacionResponse response = result.getContent().get(0);
        assertEquals(7L, response.getId());
        assertEquals(TipoNotificacion.NUEVA_RESERVA, response.getTipo());
        assertEquals("Nueva reserva", response.getMensaje());
        assertEquals(LocalDateTime.of(2026, 10, 15, 10, 0), response.getFecha());
        assertFalse(response.getLeida());
    }
}
