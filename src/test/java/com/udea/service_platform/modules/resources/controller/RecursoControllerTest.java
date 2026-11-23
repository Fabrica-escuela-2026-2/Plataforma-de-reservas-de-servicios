package com.udea.service_platform.modules.resources.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.resources.dto.RecursoRequest;
import com.udea.service_platform.modules.resources.dto.RecursoResponse;
import com.udea.service_platform.modules.resources.service.LinkedService;
import com.udea.service_platform.modules.resources.service.RecursoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecursoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RecursoControllerTest {

    private static final Long PROVEEDOR_ID = 9701L;
    private static final Long ADMIN_ID = 9703L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecursoService recursoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(PROVEEDOR_ID, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private UserDetailsImpl admin() {
        return new UserDetailsImpl(ADMIN_ID, "admin@test.com", "password", "Admin", "ADMIN");
    }

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(3L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    private String json(RecursoRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private RecursoResponse response() {
        return RecursoResponse.builder().id(9711L).nombre("Taladro").idProveedor(PROVEEDOR_ID)
                .activo(true).servicios(List.of()).build();
    }

    @Test
    void crearRecurso_casoFeliz_returns201() throws Exception {
        when(recursoService.crear(any(RecursoRequest.class), eq(PROVEEDOR_ID), eq(false))).thenReturn(response());

        mockMvc.perform(post("/api/resources")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecursoRequest("Taladro", ADMIN_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9711))
                .andExpect(jsonPath("$.nombre").value("Taladro"))
                .andExpect(jsonPath("$.idProveedor").value(PROVEEDOR_ID))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.servicios").isEmpty());
    }

    @Test
    void crearRecurso_sinNombre_returns400ConCampoNombre() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecursoRequest("   ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.nombre").value("El nombre del recurso es obligatorio"));
    }

    @Test
    void crearRecurso_nombreDuplicado_returns409() throws Exception {
        when(recursoService.crear(any(RecursoRequest.class), eq(PROVEEDOR_ID), anyBoolean()))
                .thenThrow(new ConflictException("Ya existe un recurso con ese nombre"));

        mockMvc.perform(post("/api/resources")
                        .with(user(proveedor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecursoRequest("Taladro", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un recurso con ese nombre"));
    }

    @Test
    void crearRecurso_sinToken_returns401() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecursoRequest("Taladro", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearRecurso_conRolCliente_returns403() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .with(user(cliente()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecursoRequest("Taladro", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void misRecursos_returns200() throws Exception {
        when(recursoService.misRecursos(PROVEEDOR_ID)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/resources/mine").with(user(proveedor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9711));
    }

    @Test
    void listarRecursos_soloAdmin() throws Exception {
        when(recursoService.listar(null)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/resources").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Taladro"));

        mockMvc.perform(get("/api/resources").with(user(proveedor())))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactivarRecurso_casoFeliz_returns200() throws Exception {
        when(recursoService.inactivar(9711L, PROVEEDOR_ID, false)).thenReturn(response());

        mockMvc.perform(patch("/api/resources/9711/inactivate").with(user(proveedor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9711));
    }

    @Test
    void inactivarRecurso_noExiste_returns404() throws Exception {
        when(recursoService.inactivar(9711L, PROVEEDOR_ID, false))
                .thenThrow(new ResourceNotFoundException("El recurso no existe"));

        mockMvc.perform(patch("/api/resources/9711/inactivate").with(user(proveedor())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El recurso no existe"));
    }

    @Test
    void inactivarRecurso_ajeno_returns403() throws Exception {
        when(recursoService.inactivar(9711L, PROVEEDOR_ID, false))
                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                        "El recurso no pertenece al proveedor"));

        mockMvc.perform(patch("/api/resources/9711/inactivate").with(user(proveedor())))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactivarRecurso_conReservas_returns409() throws Exception {
        when(recursoService.inactivar(9711L, PROVEEDOR_ID, false))
                .thenThrow(new ConflictException(
                        "El recurso tiene reservas pendientes. Resuélvelas antes de darlo de baja."));

        mockMvc.perform(patch("/api/resources/9711/inactivate").with(user(proveedor())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "El recurso tiene reservas pendientes. Resuélvelas antes de darlo de baja."));
    }

    @Test
    void misRecursos_incluyeLosServiciosVinculados() throws Exception {
        RecursoResponse conServicio = RecursoResponse.builder().id(9712L).nombre("Silla")
                .idProveedor(PROVEEDOR_ID).activo(true)
                .servicios(List.of(new LinkedService(9801L, "Servicio A", false))).build();
        when(recursoService.misRecursos(PROVEEDOR_ID)).thenReturn(List.of(conServicio));

        mockMvc.perform(get("/api/resources/mine").with(user(proveedor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].servicios[0].id").value(9801))
                .andExpect(jsonPath("$[0].servicios[0].nombre").value("Servicio A"))
                .andExpect(jsonPath("$[0].servicios[0].activo").value(false));
    }
}
