package com.udea.service_platform.modules.reservations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-15: contrato HTTP de POST /api/reservations (roles, codigos y mensajes).
 */
@WebMvcTest(ReservaController.class)
@Import(SecurityConfig.class)
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(2L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private UserDetailsImpl admin() {
        return new UserDetailsImpl(1L, "admin@test.com", "password", "Admin", "ADMIN");
    }

    private ReservaRequest validRequest() {
        return ReservaRequest.builder()
                .idServicio(10L)
                .fecha("2026-10-20")
                .fechaInicio("14:30")
                .build();
    }

    private ReservaResponse validResponse() {
        return ReservaResponse.builder()
                .id(100L)
                .servicioNombre("Corte")
                .clienteNombre("Cliente Uno")
                .clienteCorreo("cliente@test.com")
                .fecha(LocalDate.of(2026, 10, 20))
                .fechaInicio(LocalTime.of(14, 30))
                .estado("CONFIRMADA")
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build();
    }

    @Test
    void createReservation_asCliente_returns201Confirmada() throws Exception {
        when(reservaService.createReservation(any(ReservaRequest.class), eq(2L))).thenReturn(validResponse());

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.duracionAcordada").value(30));

        verify(reservaService).createReservation(any(ReservaRequest.class), eq(2L));
    }

    @Test
    void createReservation_asProveedor_returns403ConMensajeEspecifico() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo los clientes pueden agendar citas"));

        verifyNoInteractions(reservaService);
    }

    @Test
    void createReservation_asAdmin_returns403ConMensajeEspecifico() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo los clientes pueden agendar citas"));

        verifyNoInteractions(reservaService);
    }

    @Test
    void createReservation_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    @Test
    void createReservation_withoutServiceId_returns400() throws Exception {
        String body = """
                {
                    "fecha": "2026-10-20",
                    "fechaInicio": "14:30"
                }
                """;

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idServicio").value("El ID del servicio es obligatorio"));

        verifyNoInteractions(reservaService);
    }

    @Test
    void createReservation_whenSlotWasTaken_returns409() throws Exception {
        when(reservaService.createReservation(any(ReservaRequest.class), eq(2L)))
                .thenThrow(new ConflictException("El horario ya no está disponible. Por favor elige otro horario."));

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El horario ya no está disponible. Por favor elige otro horario."));
    }

    @Test
    void createReservation_whenSlotCannotBeBooked_returns400() throws Exception {
        when(reservaService.createReservation(any(ReservaRequest.class), eq(2L)))
                .thenThrow(new IllegalArgumentException("Ese horario no se puede reservar"));

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ese horario no se puede reservar"));
    }

    @Test
    void createReservation_whenServiceIsNotAvailable_returns400() throws Exception {
        when(reservaService.createReservation(any(ReservaRequest.class), eq(2L)))
                .thenThrow(new IllegalArgumentException("El servicio no está disponible"));

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El servicio no está disponible"));
    }
}
