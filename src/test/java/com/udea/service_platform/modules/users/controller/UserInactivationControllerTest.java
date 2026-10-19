package com.udea.service_platform.modules.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.users.dto.InactivarCuentaRequest;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserInactivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetailsImpl buildUserDetails(Long id) {
        return new UserDetailsImpl(id, "user@test.com", "pass", "User", "CLIENTE", User.ESTADO_ACTIVA);
    }

    @Test
    void inactivateMe_success() throws Exception {
        UserDetailsImpl principal = buildUserDetails(5L);
        InactivarCuentaRequest req = InactivarCuentaRequest.builder().confirmar(true).build();

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta inactivada correctamente. Tu sesión fue cerrada."));

        verify(userService).inactivateAccount(eq(5L));
    }

    @Test
    void inactivateMe_emptyBody_returns400() throws Exception {
        UserDetailsImpl principal = buildUserDetails(5L);

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(userService, never()).inactivateAccount(any());
    }

    @Test
    void inactivateMe_missingBody_returns400() throws Exception {
        UserDetailsImpl principal = buildUserDetails(5L);

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verify(userService, never()).inactivateAccount(any());
    }

    @Test
    void inactivateMe_confirmFalse_returns400() throws Exception {
        UserDetailsImpl principal = buildUserDetails(5L);
        InactivarCuentaRequest req = InactivarCuentaRequest.builder().confirmar(false).build();

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        verify(userService, never()).inactivateAccount(any());
    }

    @Test
    void inactivateMe_confirmNull_returns400() throws Exception {
        UserDetailsImpl principal = buildUserDetails(5L);
        String body = "{\"confirmar\": null}";

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verify(userService, never()).inactivateAccount(any());
    }

    @Test
    void inactivateMe_serviceThrowsIllegalArgument_returns400() throws Exception {
        UserDetailsImpl principal = buildUserDetails(5L);
        InactivarCuentaRequest req = InactivarCuentaRequest.builder().confirmar(true).build();
        String errorMsg = "No puedes inactivar tu cuenta porque tienes reservas activas. Cancela o reprograma las reservas pendientes antes de darte de baja.";
        doThrow(new IllegalArgumentException(errorMsg)).when(userService).inactivateAccount(eq(5L));

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(errorMsg));
        verify(userService).inactivateAccount(eq(5L));
    }

    @Test
    @WithAnonymousUser
    void inactivateMe_unauthenticated_returns401() throws Exception {
        InactivarCuentaRequest req = InactivarCuentaRequest.builder().confirmar(true).build();

        mockMvc.perform(patch("/api/users/me/inactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
        verify(userService, never()).inactivateAccount(any());
    }
}
