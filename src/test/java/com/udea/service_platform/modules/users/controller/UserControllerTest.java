package com.udea.service_platform.modules.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.users.dto.ProviderOnboardingRequest;
import com.udea.service_platform.modules.users.dto.RoleResponse;
import com.udea.service_platform.modules.users.dto.UserProfileResponse;
import com.udea.service_platform.modules.users.dto.UserRequest;
import com.udea.service_platform.modules.users.dto.UserResponse;
import com.udea.service_platform.modules.users.service.UserService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-02/HU-05: validates the /api/users contract.
 * Uses standalone setup (no Spring Security filter chain) to focus on
 * controller -> service delegation and GlobalExceptionHandler mappings:
 * validation errors -> 400, AccessDeniedException -> 403 (never 500).
 * Role enforcement itself is locked by onboardProvider_isProtectedByPreAuthorize.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(
                5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_withValidRequest_returns201() throws Exception {
        UserRequest request = UserRequest.builder()
                .nombre("Juan")
                .correo("juan@test.com")
                .password("password123")
                .idRol(1L)
                .build();

        RoleResponse roleResponse = RoleResponse.builder().idRol(1L).nombre("Cliente").build();
        UserResponse userResponse = UserResponse.builder()
                .idUsuario(1L)
                .nombre("Juan")
                .correo("juan@test.com")
                .role(roleResponse)
                .build();

        when(userService.register(any(UserRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.correo").value("juan@test.com"))
                .andExpect(jsonPath("$.role.nombre").value("Cliente"));
    }

    @Test
    void register_withDuplicateEmail_returns400() throws Exception {
        UserRequest request = UserRequest.builder()
                .nombre("Juan")
                .correo("existing@test.com")
                .password("password123")
                .idRol(1L)
                .build();

        when(userService.register(any(UserRequest.class)))
                .thenThrow(new IllegalArgumentException("El correo ya está en uso"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El correo ya está en uso"));
    }

    @Test
    void register_withForbiddenRole_returns400() throws Exception {
        UserRequest request = UserRequest.builder()
                .nombre("Admin")
                .correo("admin@test.com")
                .password("password123")
                .idRol(3L)
                .build();

        when(userService.register(any(UserRequest.class)))
                .thenThrow(new IllegalArgumentException("El rol seleccionado no está permitido para auto-registro"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El rol seleccionado no está permitido para auto-registro"));
    }

    @Test
    void register_withIdRolNull_returns400() throws Exception {
        String requestJson = """
                {
                    "nombre": "Juan",
                    "correo": "juan@test.com",
                    "password": "password123",
                    "idRol": null
                }
                """;

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idRol").value("Debe seleccionar un rol"));
    }

    @Test
    void register_withMissingFields_returns400() throws Exception {
        String requestJson = """
                {
                    "nombre": "",
                    "correo": "",
                    "password": "",
                    "idRol": null
                }
                """;

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onboardProvider_withValidPayload_returns200() throws Exception {
        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("3001112233")
                .notasEspeciales("Barbero con 5 años de experiencia")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .idUsuario(5L)
                .nombre("Proveedor")
                .correo("proveedor@test.com")
                .telefono("3001112233")
                .idTipoProveedor(2L)
                .notasEspeciales("Barbero con 5 años de experiencia")
                .estadoCuenta("ACTIVA")
                .role(RoleResponse.builder().idRol(2L).nombre("Proveedor de Servicios").build())
                .build();

        when(userService.onboardProvider(eq(5L), any(ProviderOnboardingRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/me/provider-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(5))
                .andExpect(jsonPath("$.idTipoProveedor").value(2))
                .andExpect(jsonPath("$.telefono").value("3001112233"))
                .andExpect(jsonPath("$.estadoCuenta").value("ACTIVA"))
                .andExpect(jsonPath("$.role.nombre").value("Proveedor de Servicios"));

        verify(userService).onboardProvider(eq(5L), any(ProviderOnboardingRequest.class));
    }

    @Test
    void onboardProvider_withMissingIdTipoProveedor_returns400() throws Exception {
        String requestJson = """
                {
                    "telefono": "3001112233"
                }
                """;

        mockMvc.perform(put("/api/users/me/provider-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.idTipoProveedor").value("Debe seleccionar un tipo de proveedor"));

        verify(userService, never()).onboardProvider(any(), any());
    }

    @Test
    void onboardProvider_withBlankTelefono_returns400() throws Exception {
        String requestJson = """
                {
                    "idTipoProveedor": 2,
                    "telefono": "   "
                }
                """;

        mockMvc.perform(put("/api/users/me/provider-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.telefono").value("El teléfono es obligatorio"));

        verify(userService, never()).onboardProvider(any(), any());
    }

    @Test
    void onboardProvider_withNotasEspecialesOver255Chars_returns400() throws Exception {
        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("3001112233")
                .notasEspeciales("a".repeat(256))
                .build();

        mockMvc.perform(put("/api/users/me/provider-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.notasEspeciales")
                        .value("La descripción no puede superar 255 caracteres"));

        verify(userService, never()).onboardProvider(any(), any());
    }

    @Test
    void onboardProvider_whenAccessDenied_returns403() throws Exception {
        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(2L)
                .telefono("3001112233")
                .build();

        when(userService.onboardProvider(any(), any()))
                .thenThrow(new AccessDeniedException("Acceso denegado"));

        mockMvc.perform(put("/api/users/me/provider-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }

    @Test
    void onboardProvider_whenServiceRejects_returns400() throws Exception {
        ProviderOnboardingRequest request = ProviderOnboardingRequest.builder()
                .idTipoProveedor(999L)
                .telefono("3001112233")
                .build();

        when(userService.onboardProvider(any(), any()))
                .thenThrow(new IllegalArgumentException("El tipo de proveedor no existe"));

        mockMvc.perform(put("/api/users/me/provider-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El tipo de proveedor no existe"));
    }

    @Test
    void onboardProvider_isProtectedByPreAuthorize() throws Exception {
        Method method = UserController.class.getMethod(
                "onboardProvider", ProviderOnboardingRequest.class, UserDetailsImpl.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "onboardProvider debe conservar @PreAuthorize");
        assertEquals("hasRole('PROVEEDOR')", preAuthorize.value());
    }
}
