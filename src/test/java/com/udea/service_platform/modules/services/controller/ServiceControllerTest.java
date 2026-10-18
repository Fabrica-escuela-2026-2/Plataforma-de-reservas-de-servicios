package com.udea.service_platform.modules.services.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceController.class)
@Import(SecurityConfig.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetailsImpl authenticate() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    @Test
    void createService_withValidRequest_returns201() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Corte de cabello")
                .descripcion("Servicio de barbería")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idRecursos(1L)
                .build();

        ServiceResponse response = ServiceResponse.builder()
                .id(10L)
                .nombre("Corte de cabello")
                .descripcion("Servicio de barbería")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idProveedor(5L)
                .idRecursos(1L)
                .build();

        when(serviceService.createService(any(ServiceRequest.class), eq(5L))).thenReturn(response);

        mockMvc.perform(post("/api/services")
                        .with(user(authenticate()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Corte de cabello"))
                .andExpect(jsonPath("$.idRecursos").value(1));
    }

    @Test
    void createService_withNonExistentRecurso_returns400() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Corte de cabello")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idRecursos(999999L)
                .build();

        when(serviceService.createService(any(ServiceRequest.class), eq(5L)))
                .thenThrow(new IllegalArgumentException("El recurso seleccionado no existe"));

        mockMvc.perform(post("/api/services")
                        .with(user(authenticate()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El recurso seleccionado no existe"));
    }

    @Test
    void createService_withNullIdRecursos_returns400() throws Exception {
        String requestJson = """
                {
                    "nombre": "Corte de cabello",
                    "categoria": "Belleza",
                    "duracion": 30,
                    "precio": 25.00,
                    "idRecursos": null
                }
                """;

        mockMvc.perform(post("/api/services")
                        .with(user(authenticate()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idRecursos").value("Debe especificar el recurso"));
    }
}