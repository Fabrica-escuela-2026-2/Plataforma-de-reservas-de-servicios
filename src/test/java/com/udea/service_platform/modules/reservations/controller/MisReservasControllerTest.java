package com.udea.service_platform.modules.reservations.controller;

import com.udea.service_platform.modules.core.config.SecurityConfig;
import com.udea.service_platform.modules.core.security.JwtUtil;
import com.udea.service_platform.modules.core.security.UserDetailsImpl;
import com.udea.service_platform.modules.core.security.UserDetailsServiceImpl;
import com.udea.service_platform.modules.reservations.dto.ContactoResponse;
import com.udea.service_platform.modules.reservations.dto.MiReservaResponse;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.service.MisReservasService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-16: contrato HTTP de GET /api/reservations/mine (roles, paginacion y mensaje).
 */
@WebMvcTest(MisReservasController.class)
@Import(SecurityConfig.class)
class MisReservasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MisReservasService misReservasService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private UserDetailsImpl cliente() {
        return new UserDetailsImpl(2L, "cliente@test.com", "password", "Cliente", "CLIENTE");
    }

    private UserDetailsImpl proveedor() {
        return new UserDetailsImpl(5L, "proveedor@test.com", "password", "Proveedor", "PROVEEDOR");
    }

    private UserDetailsImpl admin() {
        return new UserDetailsImpl(1L, "admin@test.com", "password", "Admin", "ADMIN");
    }

    private MisReservasResponse misReservas(String rolContacto) {
        MiReservaResponse fila = MiReservaResponse.builder()
                .id(100L)
                .servicioNombre("Corte")
                .fecha(LocalDate.of(2026, 10, 20))
                .fechaInicio(LocalTime.of(14, 30))
                .duracionAcordada(30)
                .valorAcordado(new BigDecimal("25000.00"))
                .estado("ACTIVA")
                .pendiente(true)
                .contacto(ContactoResponse.builder()
                        .rol(rolContacto).nombre("Otra Parte").correo("otra@test.com").telefono("300").build())
                .build();
        return MisReservasResponse.builder()
                .content(List.of(fila)).number(0).size(10).totalElements(1).totalPages(1).build();
    }

    @Test
    void getMyReservations_asCliente_usaElListadoDeCliente() throws Exception {
        when(misReservasService.getMyReservationsAsClient(eq(2L), any(Pageable.class)))
                .thenReturn(misReservas("PROVEEDOR"));

        mockMvc.perform(get("/api/reservations/mine").with(user(cliente())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].servicioNombre").value("Corte"))
                .andExpect(jsonPath("$.content[0].estado").value("ACTIVA"))
                .andExpect(jsonPath("$.content[0].pendiente").value(true))
                .andExpect(jsonPath("$.content[0].contacto.rol").value("PROVEEDOR"))
                .andExpect(jsonPath("$.content[0].contacto.correo").value("otra@test.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(misReservasService, never()).getMyReservationsAsProvider(any(), any());
    }

    @Test
    void getMyReservations_asProveedor_usaElListadoDeProveedor() throws Exception {
        when(misReservasService.getMyReservationsAsProvider(eq(5L), any(Pageable.class)))
                .thenReturn(misReservas("CLIENTE"));

        mockMvc.perform(get("/api/reservations/mine").with(user(proveedor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contacto.rol").value("CLIENTE"));

        verify(misReservasService, never()).getMyReservationsAsClient(any(), any());
    }

    @Test
    void getMyReservations_sinReservas_devuelveElMensaje() throws Exception {
        MisReservasResponse vacia = MisReservasResponse.builder()
                .mensaje("Todavía no tienes reservas").content(List.of())
                .number(0).size(10).totalElements(0).totalPages(0).build();
        when(misReservasService.getMyReservationsAsClient(eq(2L), any(Pageable.class))).thenReturn(vacia);

        mockMvc.perform(get("/api/reservations/mine").with(user(cliente())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Todavía no tienes reservas"))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getMyReservations_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/reservations/mine").with(user(admin())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(misReservasService);
    }

    @Test
    void getMyReservations_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/reservations/mine"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(misReservasService);
    }

    @Test
    void getMyReservations_limitaLaPagina() throws Exception {
        when(misReservasService.getMyReservationsAsClient(eq(2L), any(Pageable.class)))
                .thenReturn(misReservas("PROVEEDOR"));

        mockMvc.perform(get("/api/reservations/mine")
                        .param("page", "-4")
                        .param("size", "9999")
                        .with(user(cliente())))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(misReservasService).getMyReservationsAsClient(eq(2L), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void getMyReservations_conTamanoCero_usaAlMenosUnElemento() throws Exception {
        when(misReservasService.getMyReservationsAsClient(eq(2L), any(Pageable.class)))
                .thenReturn(misReservas("PROVEEDOR"));

        mockMvc.perform(get("/api/reservations/mine").param("size", "0").with(user(cliente())))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(misReservasService).getMyReservationsAsClient(eq(2L), captor.capture());
        assertEquals(1, captor.getValue().getPageSize());
    }
}
