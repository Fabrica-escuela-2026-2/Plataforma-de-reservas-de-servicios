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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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
class ReservaControllerCancelTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void cancelarReserva_comoClientePropietario_devuelve200() throws Exception {
        when(reservaService.cancelReservation(100L, 5L))
                .thenReturn(respuestaCancelada());

        mockMvc.perform(patch("/api/reservations/100/cancel")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        verify(reservaService).cancelReservation(100L, 5L);
    }

    @Test
    void cancelarReserva_comoProveedorPropietario_devuelve200() throws Exception {
        when(reservaService.cancelReservation(100L, 5L))
                .thenReturn(respuestaCancelada());

        mockMvc.perform(patch("/api/reservations/100/cancel")
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        verify(reservaService).cancelReservation(100L, 5L);
    }

    @Test
    void cancelarReserva_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch("/api/reservations/100/cancel"))
                .andExpect(status().isUnauthorized());

        verify(reservaService, never()).cancelReservation(eq(100L), eq(5L));
    }

    @Test
    void cancelarReserva_conRolNoPermitido_devuelve403() throws Exception {
        mockMvc.perform(patch("/api/reservations/100/cancel")
                        .with(user(principal("ADMIN", 5L))))
                .andExpect(status().isForbidden());

        verify(reservaService, never()).cancelReservation(eq(100L), eq(5L));
    }

    @Test
    void cancelarReserva_usuarioNoPropietario_devuelve403() throws Exception {
        when(reservaService.cancelReservation(100L, 5L))
                .thenThrow(new AccessDeniedException("No tienes permiso para cancelar esta reserva"));

        mockMvc.perform(patch("/api/reservations/100/cancel")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void cancelarReserva_inexistente_devuelve404() throws Exception {
        when(reservaService.cancelReservation(100L, 5L))
                .thenThrow(new ResourceNotFoundException("La reserva no existe"));

        mockMvc.perform(patch("/api/reservations/100/cancel")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("La reserva no existe"));
    }

    @Test
    void cancelarReserva_enEstadoNoCancelable_devuelve409() throws Exception {
        when(reservaService.cancelReservation(100L, 5L))
                .thenThrow(new ConflictException("La reserva no se puede cancelar en estado CANCELADA"));

        mockMvc.perform(patch("/api/reservations/100/cancel")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La reserva no se puede cancelar en estado CANCELADA"));
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

    private ReservaResponse respuestaCancelada() {
        return ReservaResponse.builder()
                .id(100L)
                .servicioNombre("Corte")
                .clienteNombre("Cliente")
                .clienteCorreo("cliente@test.com")
                .fecha(LocalDate.of(2026, 10, 20))
                .fechaInicio(LocalTime.of(14, 30))
                .estado("CANCELADA")
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
    }
}
