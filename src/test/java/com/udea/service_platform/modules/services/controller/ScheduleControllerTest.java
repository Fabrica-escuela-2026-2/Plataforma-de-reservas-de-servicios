package com.udea.service_platform.modules.services.controller;

import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.services.dto.HorarioResponse;
import com.udea.service_platform.modules.services.service.HorarioService;
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
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-12: validates the /api/schedules contract.
 * Uses standalone setup (no Spring Security filter chain) to focus on
 * controller -> service delegation and GlobalExceptionHandler mappings:
 * unreadable body / bad path variable -> 400 (never 500), AccessDeniedException -> 403.
 * Role enforcement itself is locked by defineSchedule_isProtectedByPreAuthorize.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    private static final String BASE_URL = "/api/schedules/service/10";
    private static final String PAYLOAD_VALIDO =
            "[{\"idDiaSemana\":1,\"horaInicio\":\"09:00\",\"horaFin\":\"11:00\"}]";

    @Mock
    private HorarioService horarioService;

    @InjectMocks
    private ScheduleController scheduleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(
                5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc = MockMvcBuilders.standaloneSetup(scheduleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void defineSchedule_withValidPayload_returns200AndDelegates() throws Exception {
        when(horarioService.defineSchedule(eq(10L), any(), eq(5L)))
                .thenReturn(List.of(response()));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idHorario").value(1))
                .andExpect(jsonPath("$[0].idDiaSemana").value(1))
                .andExpect(jsonPath("$[0].nombreDia").value("Lunes"))
                .andExpect(jsonPath("$[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$[0].horaFin").value("11:00:00"))
                .andExpect(jsonPath("$[0].idServicio").value(10));

        verify(horarioService).defineSchedule(eq(10L), any(), eq(5L));
    }

    @Test
    void defineSchedule_whenServiceRejectsEmptyList_returns400() throws Exception {
        when(horarioService.defineSchedule(eq(10L), any(), eq(5L)))
                .thenThrow(new IllegalArgumentException("Debe definir al menos un horario para el servicio"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe definir al menos un horario para el servicio"));
    }

    @Test
    void defineSchedule_whenAccessDenied_returns403() throws Exception {
        when(horarioService.defineSchedule(eq(10L), any(), eq(5L)))
                .thenThrow(new AccessDeniedException("No tienes permiso para gestionar el horario de este servicio"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void defineSchedule_withUnparseableTime_returns400Not500() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"idDiaSemana\":1,\"horaInicio\":\"25:99\",\"horaFin\":\"10:00\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cuerpo de la petición inválido"));

        verifyNoInteractions(horarioService);
    }

    @Test
    void getSchedule_withExistingRows_returns200() throws Exception {
        when(horarioService.getSchedule(10L, 5L)).thenReturn(List.of(response()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreDia").value("Lunes"))
                .andExpect(jsonPath("$[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$[0].idServicio").value(10));
    }

    @Test
    void getSchedule_withoutRows_returns200WithEmptyArray() throws Exception {
        when(horarioService.getSchedule(10L, 5L)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSchedule_withNonNumericServiceId_returns400Not500() throws Exception {
        mockMvc.perform(get("/api/schedules/service/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'serviceId' tiene un formato inválido"));

        verifyNoInteractions(horarioService);
    }

    @Test
    void defineSchedule_isProtectedByPreAuthorize() throws Exception {
        Method method = ScheduleController.class.getMethod(
                "defineSchedule", Long.class, List.class, UserDetailsImpl.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('PROVEEDOR')", preAuthorize.value());
    }

    @Test
    void getSchedule_isProtectedByPreAuthorize() throws Exception {
        Method method = ScheduleController.class.getMethod(
                "getSchedule", Long.class, UserDetailsImpl.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('PROVEEDOR')", preAuthorize.value());
    }

    private HorarioResponse response() {
        return HorarioResponse.builder()
                .idHorario(1)
                .idDiaSemana(1)
                .nombreDia("Lunes")
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0))
                .idServicio(10L)
                .build();
    }
}
