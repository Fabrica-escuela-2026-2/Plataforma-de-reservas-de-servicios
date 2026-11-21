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
class ReservaControllerRescheduleTest {

    private static final String URL = "/api/reservations/100/reschedule";
    private static final String BODY_VALIDO =
            "{\"fecha\":\"2026-10-21\",\"fechaInicio\":\"10:00\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void reprogramarReserva_comoClientePropietario_devuelve200() throws Exception {
        when(reservaService.rescheduleReservation(eq(100L),
                argThat(r -> "2026-10-21".equals(r.getFecha())
                        && "10:00".equals(r.getFechaInicio())), eq(5L)))
                .thenReturn(respuesta());

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.fecha").value("2026-10-21"))
                .andExpect(jsonPath("$.fechaInicio").value("10:00:00"));

        verify(reservaService).rescheduleReservation(eq(100L),
                argThat(r -> "2026-10-21".equals(r.getFecha())
                        && "10:00".equals(r.getFechaInicio())), eq(5L));
    }

    @Test
    void reprogramarReserva_comoProveedorPropietario_devuelve200() throws Exception {
        when(reservaService.rescheduleReservation(eq(100L), any(), eq(5L)))
                .thenReturn(respuesta());

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("PROVEEDOR", 5L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void reprogramarReserva_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized());

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
    }

    @Test
    void reprogramarReserva_comoAdmin_devuelve403() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("ADMIN", 5L))))
                .andExpect(status().isForbidden());

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
    }

    @Test
    void reprogramarReserva_conUsuarioNoPropietario_devuelve403() throws Exception {
        when(reservaService.rescheduleReservation(any(), any(), any()))
                .thenThrow(new AccessDeniedException(
                        "No tienes permiso para reprogramar esta reserva"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void reprogramarReserva_inexistente_devuelve404() throws Exception {
        when(reservaService.rescheduleReservation(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("La reserva no existe"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("La reserva no existe"));
    }

    @Test
    void reprogramarReserva_enEstadoNoReprogramable_devuelve409() throws Exception {
        when(reservaService.rescheduleReservation(any(), any(), any()))
                .thenThrow(new ConflictException(
                        "La reserva no se puede reprogramar en estado EN_PROCESO"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("La reserva no se puede reprogramar en estado EN_PROCESO"));
    }

    @Test
    void reprogramarReserva_conFranjaNoDisponible_devuelve409() throws Exception {
        when(reservaService.rescheduleReservation(any(), any(), any()))
                .thenThrow(new ConflictException("El horario seleccionado ya está ocupado"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_VALIDO)
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El horario seleccionado ya está ocupado"));
    }

    @Test
    void reprogramarReserva_sinFecha_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"10:00\"}")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fecha").value("La fecha es obligatoria"));

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
    }

    @Test
    void reprogramarReserva_sinHora_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-21\"}")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fechaInicio")
                        .value("La fecha de inicio es obligatoria"));

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
    }

    @Test
    void reprogramarReserva_conFechaMalFormada_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"21/10/2026\",\"fechaInicio\":\"10:00\"}")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fecha")
                        .value("Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)"));

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
    }

    @Test
    void reprogramarReserva_conHoraMalFormada_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-10-21\",\"fechaInicio\":\"25:99\"}")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fechaInicio")
                        .value("Formato de hora inválido. Use HH:mm (ej. 14:30)"));

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
    }

    @Test
    void reprogramarReserva_conJsonMalFormado_devuelve400() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{fecha:2026-10-21}")
                        .with(user(principal("CLIENTE", 5L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cuerpo de la petición inválido"));

        verify(reservaService, never()).rescheduleReservation(any(), any(), any());
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

    private ReservaResponse respuesta() {
        return ReservaResponse.builder()
                .id(100L)
                .servicioNombre("Corte")
                .clienteNombre("Cliente")
                .clienteCorreo("cliente@test.com")
                .fecha(LocalDate.of(2026, 10, 21))
                .fechaInicio(LocalTime.of(10, 0))
                .estado("ACTIVA")
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(45)
                .build();
    }
}
