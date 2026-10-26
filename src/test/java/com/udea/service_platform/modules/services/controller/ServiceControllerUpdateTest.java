package com.udea.service_platform.modules.services.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.dto.ServiceRequest;
import com.udea.service_platform.modules.services.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ServiceControllerUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(2L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    @Test
    void updateService_duenoProveedor_returns200WithUpdatedData() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure Premium")
                .descripcion("Manicure con esmaltado")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("35000.00"))
                .idRecursos(1L)
                .build();

        ServiceResponse response = ServiceResponse.builder()
                .id(10L)
                .nombre("Manicure Premium")
                .descripcion("Manicure con esmaltado")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("35000.00"))
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(true)
                .build();

        when(serviceService.updateService(eq(10L), any(ServiceRequest.class), eq(5L)))
                .thenReturn(response);

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Manicure Premium"))
                .andExpect(jsonPath("$.duracion").value(45))
                .andExpect(jsonPath("$.precio").value(35000.00))
                .andExpect(jsonPath("$.idProveedor").value(5))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void updateService_bodyInvalido_precioCero_returns400() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("0.00"))
                .idRecursos(1L)
                .build();

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.precio").exists());
    }

    @Test
    void updateService_bodyInvalido_duracionCero_returns400() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(0)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.duracion").exists());
    }

    @Test
    void updateService_bodyInvalido_nombreVacio_returns400() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.nombre").exists());
    }

    @Test
    void updateService_bodyInvalido_categoriaVacia_returns400() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.categoria").exists());
    }

    @Test
    void updateService_bodyInvalido_idRecursosNull_returns400() throws Exception {
        String requestJson = """
                {
                    "nombre": "Manicure",
                    "categoria": "Belleza",
                    "duracion": 45,
                    "precio": 25000.00,
                    "idRecursos": null
                }
                """;

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idRecursos").value("Debe especificar el recurso"));
    }

    @Test
    void updateService_usuarioCliente_returns403() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        mockMvc.perform(put("/api/services/10")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateService_servicioInexistente_returns404WithErrorMessage() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        when(serviceService.updateService(eq(10L), any(ServiceRequest.class), eq(5L)))
                .thenThrow(new ResourceNotFoundException("El servicio no existe"));

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El servicio no existe"));
    }

    @Test
    void updateService_otroProveedor_returns403() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        when(serviceService.updateService(eq(10L), any(ServiceRequest.class), eq(5L)))
                .thenThrow(new AccessDeniedException("El servicio no pertenece al proveedor"));

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void updateService_idProveedorProvieneDelToken_noDelBody() throws Exception {
        ServiceRequest request = ServiceRequest.builder()
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idRecursos(1L)
                .build();

        ServiceResponse response = ServiceResponse.builder()
                .id(10L)
                .nombre("Manicure")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(true)
                .build();

        when(serviceService.updateService(eq(10L), any(ServiceRequest.class), eq(5L)))
                .thenReturn(response);

        mockMvc.perform(put("/api/services/10")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProveedor").value(5));
    }
}
