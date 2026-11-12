package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.service.ReservaService;
import com.udea.service_platform.modules.users.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReservaControllerStatusUpdateTest {

    private static final String URL = "/api/reservations/100/status";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void actualizarEstado_comoProveedorPropietario_devuelve200() throws Exception {
        when(reservaService.updateStatus(eq(100L),
                argThat(r -> "CONFIRMADA".equals(r.getEstado())), eq(5L)))
                .thenReturn(respuesta("CONFIRMADA"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        verify(reservaService).updateStatus(eq(100L),
                argThat(r -> "CONFIRMADA".equals(r.getEstado())), eq(5L));
    }

    @Test
    void actualizarEstado_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}"))
                .andExpect(status().isUnauthorized());

        verify(reservaService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void actualizarEstado_comoCliente_devuelve403() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isForbidden());

        verify(reservaService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void actualizarEstado_comoAdmin_devuelve403() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}")
                        .with(user(principal("ADMIN", 5L))))
                .andExpect(status().isForbidden());

        verify(reservaService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void actualizarEstado_conProveedorAjeno_devuelve403() throws Exception {
        when(reservaService.updateStatus(any(), any(), any()))
                .thenThrow(new AccessDeniedException(
                        "No tienes permiso para actualizar esta reserva"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void actualizarEstado_inexistente_devuelve404() throws Exception {
        when(reservaService.updateStatus(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("La reserva no existe"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("La reserva no existe"));
    }

    @Test
    void actualizarEstado_conTransicionInvalida_devuelve409() throws Exception {
        when(reservaService.updateStatus(any(), any(), any()))
                .thenThrow(new ConflictException(
                        "Transición no permitida: EN_PROCESO -> CONFIRMADA"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CONFIRMADA\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Transición no permitida: EN_PROCESO -> CONFIRMADA"));
    }

    @Test
    void actualizarEstado_enEstadoTerminal_devuelve409() throws Exception {
        when(reservaService.updateStatus(any(), any(), any()))
                .thenThrow(new ConflictException(
                        "No se puede cambiar el estado de una reserva en estado COMPLETADA"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROCESO\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("No se puede cambiar el estado de una reserva en estado COMPLETADA"));
    }

    @Test
    void actualizarEstado_aCancelada_devuelve409() throws Exception {
        when(reservaService.updateStatus(any(), any(), any()))
                .thenThrow(new ConflictException(
                        "Usa /api/reservations/{id}/cancel para cancelar una reserva"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Usa /api/reservations/{id}/cancel para cancelar una reserva"));
    }

    @Test
    void actualizarEstado_sinEstado_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.estado").value("El estado es obligatorio"));

        verify(reservaService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void actualizarEstado_conEstadoEnBlanco_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"   \"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.estado").value("El estado es obligatorio"));

        verify(reservaService, never()).updateStatus(any(), any(), any());
    }

    @Test
    void actualizarEstado_conEstadoDesconocido_devuelve400() throws Exception {
        when(reservaService.updateStatus(any(), any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "Estado no válido: 'PAUSADA'. Valores permitidos: CONFIRMADA, EN_PROCESO, COMPLETADA, NO_ASISTIO"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PAUSADA\"}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Estado no válido: 'PAUSADA'. Valores permitidos: CONFIRMADA, EN_PROCESO, COMPLETADA, NO_ASISTIO"));
    }

    @Test
    void actualizarEstado_conJsonMalFormado_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{estado:CONFIRMADA}")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cuerpo de la petición inválido"));

        verify(reservaService, never()).updateStatus(any(), any(), any());
    }

    private UserDetailsImpl principal(String role, Long idUsuario) {
        return new UserDetailsImpl(
                idUsuario,
                "user@test.com",
                "pass",
                "User",
                role,
                User.ESTADO_ACTIVA);
    }

    private ReservaResponse respuesta(String estado) {
        return ReservaResponse.builder()
                .id(100L)
                .servicioNombre("Corte")
                .clienteNombre("Cliente")
                .clienteCorreo("cliente@test.com")
                .fecha(LocalDate.of(2026, 10, 20))
                .fechaInicio(LocalTime.of(14, 30))
                .estado(estado)
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
    }
}
