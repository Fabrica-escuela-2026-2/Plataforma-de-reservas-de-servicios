package com.udea.service_platform.modules.reservations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ReservaControllerTest {

    private static final Long CLIENTE_ID = 2L;

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
        return new UserDetailsImpl(CLIENTE_ID, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private ReservaRequest requestValido() {
        return ReservaRequest.builder()
                .idServicio(10L)
                .fecha("2026-10-20")
                .fechaInicio("14:30")
                .build();
    }

    private String json(ReservaRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void crearReserva_servicioInexistente_returns404WithErrorMessage() throws Exception {
        when(reservaService.createReservation(any(ReservaRequest.class), eq(CLIENTE_ID)))
                .thenThrow(new ResourceNotFoundException("El servicio no existe"));

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(requestValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El servicio no existe"));
    }

    @Test
    void crearReserva_servicioInactivo_returns400WithErrorMessage() throws Exception {
        when(reservaService.createReservation(any(ReservaRequest.class), eq(CLIENTE_ID)))
                .thenThrow(new IllegalArgumentException("El servicio no está activo"));

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(requestValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El servicio no está activo"));
    }

    @Test
    void crearReserva_sinToken_returns401() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(requestValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearReserva_conRolProveedor_returns403() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(requestValido())))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearReserva_casoFeliz_returns201WithReservation() throws Exception {
        ReservaResponse response = ReservaResponse.builder()
                .id(50L)
                .servicioNombre("Consulta general")
                .clienteNombre("Cliente")
                .clienteCorreo("cliente@test.com")
                .fechaInicio(LocalTime.of(14, 30))
                .fecha(LocalDate.of(2026, 10, 20))
                .estado("ACTIVA")
                .valorAcordado(new BigDecimal("50000"))
                .duracionAcordada(60)
                .build();

        when(reservaService.createReservation(any(ReservaRequest.class), eq(CLIENTE_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reservations")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.servicioNombre").value("Consulta general"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }
}
