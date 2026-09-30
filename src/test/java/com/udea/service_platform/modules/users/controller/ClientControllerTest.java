package com.udea.service_platform.modules.users.controller;

import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.reservations.service.ReservaService;
import com.udea.service_platform.modules.users.dto.ClientDetailResponse;
import com.udea.service_platform.modules.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU-04: validates the GET /api/clients/{id} contract.
 * Uses standalone setup (no Spring Security filter chain) to focus on
 * controller -> service delegation and GlobalExceptionHandler mappings:
 * valid Cliente -> 200, non-Cliente / unknown id -> 400 (never 500).
 */
@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getClientDetail_withClienteId_returns200() throws Exception {
        ClientDetailResponse detail = ClientDetailResponse.builder()
                .idUsuario(6L)
                .nombre("Juan")
                .apellido("Pérez")
                .correo("juan@test.com")
                .telefono("3001234567")
                .numeroDocumento("123456789")
                .idTipoDocumento(1L)
                .idCiudad(1L)
                .estadoCuenta("ACTIVA")
                .build();
        when(userService.getClientDetail(6L)).thenReturn(detail);

        mockMvc.perform(get("/api/clients/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(6))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.correo").value("juan@test.com"));
    }

    @Test
    void getClientDetail_withProveedorId_returns400() throws Exception {
        when(userService.getClientDetail(2L))
                .thenThrow(new IllegalArgumentException("El usuario no tiene rol de Cliente"));

        mockMvc.perform(get("/api/clients/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El usuario no tiene rol de Cliente"));
    }

    @Test
    void getClientDetail_withNonExistentId_returns400() throws Exception {
        when(userService.getClientDetail(999L))
                .thenThrow(new IllegalArgumentException("Usuario no encontrado"));

        mockMvc.perform(get("/api/clients/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Usuario no encontrado"));
    }
}
