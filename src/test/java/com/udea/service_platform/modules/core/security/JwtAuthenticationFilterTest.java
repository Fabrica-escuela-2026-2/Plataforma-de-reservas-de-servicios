package com.udea.service_platform.modules.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filter_activaAuthenticates() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractCorreo(token)).thenReturn("active@test.com");
        UserDetailsImpl activeUser = new UserDetailsImpl(1L, "active@test.com", "pass", "Active", "CLIENTE",
                com.udea.service_platform.modules.users.model.User.ESTADO_ACTIVA);
        when(userDetailsService.loadUserByUsername("active@test.com")).thenReturn(activeUser);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        filter.doFilter(request, response, chain);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("active@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void filter_inactivaDoesNotAuthenticate() throws ServletException, IOException {
        String token = "valid-token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.extractCorreo(token)).thenReturn("inactive@test.com");
        UserDetailsImpl inactiveUser = new UserDetailsImpl(2L, "inactive@test.com", "pass", "Inactive", "CLIENTE",
                com.udea.service_platform.modules.users.model.User.ESTADO_INACTIVA);
        when(userDetailsService.loadUserByUsername("inactive@test.com")).thenReturn(inactiveUser);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        filter.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
