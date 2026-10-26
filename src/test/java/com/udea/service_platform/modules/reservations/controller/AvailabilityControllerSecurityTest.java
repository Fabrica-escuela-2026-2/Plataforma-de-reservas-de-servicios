package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.reservations.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvailabilityController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AvailabilityControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getAvailability_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/availability").param("serviceId", "10").param("fecha", "2026-10-20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAvailability_conTokenValido_devuelve200() throws Exception {
        mockMvc.perform(get("/api/availability").param("serviceId", "10").param("fecha", "2026-10-20")
                        .with(SecurityMockMvcRequestPostProcessors.securityContext(contextoAutenticado())))
                .andExpect(status().isOk());
    }

    private SecurityContext contextoAutenticado() {
        UserDetails user = User.builder()
                .username("cliente@test.com")
                .password("pass")
                .roles("CLIENTE")
                .build();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, user.getPassword(), user.getAuthorities()));
        return context;
    }
}
