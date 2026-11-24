package com.udea.service_platform.modules.services.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.services.dto.AforoRequest;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceController.class)
@Import(SecurityConfig.class)
class ServiceAforoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updateAforo_conProveedorAutorizado_devuelve200ConElNuevoAforo() throws Exception {
        when(serviceService.updateAforo(eq(9908L), eq(3), eq(5L))).thenReturn(response(3));

        mockMvc.perform(patch("/api/services/9908/aforo")
                        .with(user(authenticate("PROVEEDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AforoRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9908))
                .andExpect(jsonPath("$.aforo").value(3));

        verify(serviceService).updateAforo(9908L, 3, 5L);
    }

    @Test
    void updateAforo_conCliente_devuelve403() throws Exception {
        mockMvc.perform(patch("/api/services/9908/aforo")
                        .with(user(authenticate("CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AforoRequest(2))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateAforo_servicioInexistente_devuelve404() throws Exception {
        when(serviceService.updateAforo(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("El servicio no existe"));

        mockMvc.perform(patch("/api/services/9908/aforo")
                        .with(user(authenticate("PROVEEDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AforoRequest(2))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El servicio no existe"));
    }

    @Test
    void updateAforo_servideoDeOtroProveedor_devuelve403() throws Exception {
        when(serviceService.updateAforo(any(), any(), any()))
                .thenThrow(new AccessDeniedException("El servicio no pertenece al proveedor"));

        mockMvc.perform(patch("/api/services/9908/aforo")
                        .with(user(authenticate("PROVEEDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AforoRequest(2))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void updateAforo_valorInvalido_devuelve400() throws Exception {
        when(serviceService.updateAforo(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("El aforo debe ser de al menos un cupo"));

        mockMvc.perform(patch("/api/services/9908/aforo")
                        .with(user(authenticate("PROVEEDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AforoRequest(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El aforo debe ser de al menos un cupo"));
    }

    @Test
    void updateAforo_picoSuperaElAforo_devuelve409() throws Exception {
        when(serviceService.updateAforo(any(), any(), any()))
                .thenThrow(new ConflictException("Ya existen más reservas que el nuevo aforo"));

        mockMvc.perform(patch("/api/services/9908/aforo")
                        .with(user(authenticate("PROVEEDOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AforoRequest(1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existen más reservas que el nuevo aforo"));
    }

    private UserDetailsImpl authenticate(String role) {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", role);
    }

    private ServiceResponse response(Integer aforo) {
        return ServiceResponse.builder()
                .id(9908L)
                .nombre("Corte")
                .descripcion("Barbería")
                .categoria("Belleza")
                .duracion(45)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(5L)
                .idRecursos(9907L)
                .aforo(aforo)
                .activo(true)
                .build();
    }
}
