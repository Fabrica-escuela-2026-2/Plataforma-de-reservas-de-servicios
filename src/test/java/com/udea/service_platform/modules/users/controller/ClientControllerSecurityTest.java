package com.udea.service_platform.modules.users.controller;

import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.reservations.service.ReservaService;
import com.udea.service_platform.modules.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ClientControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getClients_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getClients_withInvalidToken_returns401() throws Exception {
        when(jwtUtil.isTokenValid("fake.jwt.token")).thenReturn(false);
        mockMvc.perform(get("/api/clients")
                        .header("Authorization", "Bearer fake.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getClients_asCliente_returns403() throws Exception {
        UserDetails cliente = User.builder()
                .username("cliente@test.com")
                .password("pass")
                .roles("CLIENTE")
                .build();

        mockMvc.perform(get("/api/clients?searchTerm=a")
                        .with(authenticatedAs(cliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClientDetail_asCliente_returns403() throws Exception {
        UserDetails cliente = User.builder()
                .username("cliente@test.com")
                .password("pass")
                .roles("CLIENTE")
                .build();

        mockMvc.perform(get("/api/clients/2")
                        .with(authenticatedAs(cliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getClients_asProveedor_returns200() throws Exception {
        UserDetails proveedor = User.builder()
                .username("proveedor@test.com")
                .password("pass")
                .roles("PROVEEDOR")
                .build();
        when(userService.searchClients(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/clients?searchTerm=a")
                        .with(authenticatedAs(proveedor)))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor authenticatedAs(UserDetails userDetails) {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    userDetails, userDetails.getPassword(), userDetails.getAuthorities()));
            request.setAttribute(RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME, context);
            return request;
        };
    }
}