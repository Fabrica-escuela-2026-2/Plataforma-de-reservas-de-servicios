package com.udea.service_platform.modules.services.controller;

import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.services.dto.BloqueoRequest;
import com.udea.service_platform.modules.services.dto.BloqueoResponse;
import com.udea.service_platform.modules.services.service.BloqueoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-13: validates the /api/blocks contract.
 * Uses standalone setup (no Spring Security filter chain) to focus on
 * controller -> service delegation, @Valid field errors -> 400 fields.*,
 * business rejections -> 400 {"error":...}, AccessDeniedException -> 403.
 * Role enforcement itself is locked by the three isProtectedByPreAuthorize tests.
 */
@ExtendWith(MockitoExtension.class)
class BloqueoControllerTest {

    private static final String PROVIDER_URL = "/api/blocks/provider";

    @Mock
    private BloqueoService bloqueoService;

    @InjectMocks
    private BloqueoController bloqueoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(
                5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc = MockMvcBuilders.standaloneSetup(bloqueoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBlock_fullDay_returns201AndDelegates() throws Exception {
        when(bloqueoService.createBlock(any(), eq(5L))).thenReturn(fullDayResponse());

        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-15\",\"motivo\":\"Vacaciones\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idBloqueo").value(1))
                .andExpect(jsonPath("$.idUsuarioProveedor").value(5))
                .andExpect(jsonPath("$.fecha").value("2027-01-15"))
                .andExpect(jsonPath("$.horaInicio").doesNotExist())
                .andExpect(jsonPath("$.horaFin").doesNotExist())
                .andExpect(jsonPath("$.motivo").value("Vacaciones"));

        verify(bloqueoService).createBlock(any(), eq(5L));
    }

    @Test
    void createBlock_timeRange_returns201() throws Exception {
        when(bloqueoService.createBlock(any(), eq(5L))).thenReturn(timedResponse());

        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-16\",\"horaInicio\":\"09:00:00\",\"horaFin\":\"11:00:00\",\"motivo\":\"Cita médica\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.horaFin").value("11:00:00"))
                .andExpect(jsonPath("$.motivo").value("Cita médica"));
    }

    @Test
    void createBlock_pastDate_returns400() throws Exception {
        when(bloqueoService.createBlock(any(), eq(5L)))
                .thenThrow(new IllegalArgumentException("La fecha no puede ser en el pasado"));

        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2020-01-01\",\"motivo\":\"Ayuno\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha no puede ser en el pasado"));
    }

    @Test
    void createBlock_partialTimes_returns400() throws Exception {
        when(bloqueoService.createBlock(any(), eq(5L)))
                .thenThrow(new IllegalArgumentException("Debe indicar ambas horas (inicio y fin) o ninguna"));

        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-16\",\"horaInicio\":\"10:00:00\",\"motivo\":\"Parcial\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe indicar ambas horas (inicio y fin) o ninguna"));
    }

    @Test
    void createBlock_blankMotivo_returns400WithFieldError() throws Exception {
        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-17\",\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"))
                .andExpect(jsonPath("$.fields.motivo").value("El motivo es obligatorio"));

        verifyNoInteractions(bloqueoService);
    }

    @Test
    void createBlock_missingFecha_returns400WithFieldError() throws Exception {
        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Vacaciones\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fecha").value("La fecha es obligatoria"));

        verifyNoInteractions(bloqueoService);
    }

    @Test
    void createBlock_whenAccessDenied_returns403() throws Exception {
        when(bloqueoService.createBlock(any(), eq(5L)))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(post(PROVIDER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2027-01-18\",\"motivo\":\"Vacaciones\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveBlocks_returns200() throws Exception {
        when(bloqueoService.getActiveBlocks(5L)).thenReturn(List.of(fullDayResponse(), timedResponse()));

        mockMvc.perform(get(PROVIDER_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fecha").value("2027-01-15"))
                .andExpect(jsonPath("$[1].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$[1].motivo").value("Cita médica"));
    }

    @Test
    void getActiveBlocks_withoutRows_returns200WithEmptyArray() throws Exception {
        when(bloqueoService.getActiveBlocks(5L)).thenReturn(List.of());

        mockMvc.perform(get(PROVIDER_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteBlock_returns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete("/api/blocks/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(bloqueoService).deleteBlock(10L, 5L);
    }

    @Test
    void deleteBlock_whenAccessDenied_returns403() throws Exception {
        doThrow(new AccessDeniedException("El bloque no existe o no tienes permiso sobre él"))
                .when(bloqueoService).deleteBlock(10L, 5L);

        mockMvc.perform(delete("/api/blocks/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void createBlock_isProtectedByPreAuthorize() throws Exception {
        Method method = BloqueoController.class.getMethod(
                "createBlock", BloqueoRequest.class, UserDetailsImpl.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('PROVEEDOR')", preAuthorize.value());
    }

    @Test
    void getActiveBlocks_isProtectedByPreAuthorize() throws Exception {
        Method method = BloqueoController.class.getMethod(
                "getActiveBlocks", UserDetailsImpl.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('PROVEEDOR')", preAuthorize.value());
    }

    @Test
    void deleteBlock_isProtectedByPreAuthorize() throws Exception {
        Method method = BloqueoController.class.getMethod(
                "deleteBlock", Long.class, UserDetailsImpl.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('PROVEEDOR')", preAuthorize.value());
    }

    private BloqueoResponse fullDayResponse() {
        return BloqueoResponse.builder()
                .idBloqueo(1L)
                .idUsuarioProveedor(5L)
                .fecha(LocalDate.parse("2027-01-15"))
                .motivo("Vacaciones")
                .build();
    }

    private BloqueoResponse timedResponse() {
        return BloqueoResponse.builder()
                .idBloqueo(2L)
                .idUsuarioProveedor(5L)
                .fecha(LocalDate.parse("2027-01-16"))
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0))
                .motivo("Cita médica")
                .build();
    }
}
