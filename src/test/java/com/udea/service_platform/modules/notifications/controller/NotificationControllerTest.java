package com.udea.service_platform.modules.notifications.controller;

import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.notifications.dto.NotificacionResponse;
import com.udea.service_platform.modules.notifications.model.TipoNotificacion;
import com.udea.service_platform.modules.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(2L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    private NotificacionResponse notificacion() {
        return NotificacionResponse.builder()
                .id(1L)
                .tipo(TipoNotificacion.RESERVA_CONFIRMADA)
                .mensaje("Tu reserva quedó confirmada")
                .fecha(LocalDateTime.of(2026, 10, 15, 10, 0))
                .leida(false)
                .build();
    }

    @Test
    void getMyNotifications_devuelveLasDelUsuarioAutenticado() throws Exception {
        when(notificationService.getUserNotifications(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificacion())));

        mockMvc.perform(get("/api/notifications").with(user(cliente())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("RESERVA_CONFIRMADA"))
                .andExpect(jsonPath("$.content[0].mensaje").value("Tu reserva quedó confirmada"))
                .andExpect(jsonPath("$.content[0].leida").value(false));
    }

    @Test
    void getMyNotifications_limitaElTamanoDePagina() throws Exception {
        when(notificationService.getUserNotifications(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/notifications")
                        .param("page", "-3")
                        .param("size", "5000")
                        .with(user(cliente())))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService).getUserNotifications(eq(2L), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void getMyNotifications_conTamanoCero_usaAlMenosUnElemento() throws Exception {
        when(notificationService.getUserNotifications(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/notifications").param("size", "0").with(user(cliente())))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationService).getUserNotifications(eq(2L), captor.capture());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    void getMyNotifications_sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }
}
