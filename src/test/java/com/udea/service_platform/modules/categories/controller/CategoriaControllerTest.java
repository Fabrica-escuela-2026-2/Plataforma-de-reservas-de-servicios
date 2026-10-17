package com.udea.service_platform.modules.categories.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.categories.dto.CategoriaRequest;
import com.udea.service_platform.modules.categories.dto.CategoriaResponse;
import com.udea.service_platform.modules.categories.service.CategoriaService;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@Import(SecurityConfig.class)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private UserDetailsImpl admin() {
        return new UserDetailsImpl(1L, "admin@test.com", "password", "Admin", "ADMIN");
    }

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(9L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    private CategoriaRequest validRequest() {
        return CategoriaRequest.builder()
                .nombre("Estética")
                .descripcion("Servicios de belleza")
                .build();
    }

    private CategoriaResponse validResponse() {
        return CategoriaResponse.builder()
                .id(1L)
                .nombre("Estética")
                .descripcion("Servicios de belleza")
                .build();
    }

    @Test
    void createCategory_asProveedor_returns201() throws Exception {
        when(categoriaService.createCategory(any(CategoriaRequest.class))).thenReturn(validResponse());

        mockMvc.perform(post("/api/categories")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Estética"))
                .andExpect(jsonPath("$.descripcion").value("Servicios de belleza"));
    }

    @Test
    void createCategory_asAdmin_returns201() throws Exception {
        when(categoriaService.createCategory(any(CategoriaRequest.class))).thenReturn(validResponse());

        mockMvc.perform(post("/api/categories")
                        .with(user(admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void createCategory_asCliente_returns403() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoriaService);
    }

    @Test
    void createCategory_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(categoriaService);
    }

    @Test
    void createCategory_withBlankName_returns400() throws Exception {
        String requestJson = """
                {
                    "nombre": "   ",
                    "descripcion": "Servicios de belleza"
                }
                """;

        mockMvc.perform(post("/api/categories")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.nombre").value("El nombre es obligatorio"));

        verifyNoInteractions(categoriaService);
    }

    @Test
    void createCategory_withDuplicateName_returns400() throws Exception {
        when(categoriaService.createCategory(any(CategoriaRequest.class)))
                .thenThrow(new IllegalArgumentException("La categoría ya existe"));

        mockMvc.perform(post("/api/categories")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La categoría ya existe"));
    }

    @Test
    void getAllCategories_asCliente_returns200() throws Exception {
        when(categoriaService.getAllCategories()).thenReturn(List.of(validResponse()));

        mockMvc.perform(get("/api/categories").with(user(cliente())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Estética"));
    }

    @Test
    void getAllCategories_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(categoriaService);
    }
}
