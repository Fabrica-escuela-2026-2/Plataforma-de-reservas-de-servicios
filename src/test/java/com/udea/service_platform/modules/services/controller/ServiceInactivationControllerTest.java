package com.udea.service_platform.modules.services.controller;

import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.services.service.ServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceController.class)
@Import(SecurityConfig.class)
class ServiceInactivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(9L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    @Test
    void inactivateService_asOwnerProvider_returns200WithInactiveService() throws Exception {
        ServiceResponse response = ServiceResponse.builder()
                .id(10L)
                .nombre("Corte de cabello")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idProveedor(5L)
                .idRecursos(1L)
                .activo(false)
                .build();

        when(serviceService.inactivateService(10L, 5L)).thenReturn(response);

        mockMvc.perform(patch("/api/services/10/inactivate").with(user(proveedor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void inactivateService_nonExistentService_returns404() throws Exception {
        when(serviceService.inactivateService(999L, 5L))
                .thenThrow(new ResourceNotFoundException("El servicio no existe"));

        mockMvc.perform(patch("/api/services/999/inactivate").with(user(proveedor())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El servicio no existe"));
    }

    @Test
    void inactivateService_serviceOfAnotherProvider_returns403() throws Exception {
        when(serviceService.inactivateService(10L, 5L))
                .thenThrow(new AccessDeniedException("El servicio no pertenece al proveedor"));

        mockMvc.perform(patch("/api/services/10/inactivate").with(user(proveedor())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void inactivateService_asCliente_returns403() throws Exception {
        mockMvc.perform(patch("/api/services/10/inactivate").with(user(cliente())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(serviceService);
    }

    @Test
    void inactivateService_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(patch("/api/services/10/inactivate"))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(serviceService);
    }
}
