package com.udea.service_platform.modules.reservations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.modules.core.exception.GlobalExceptionHandler;
import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.AvailabilitySlotResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    private static final String BASE_URL = "/api/availability";

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AvailabilityController availabilityController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sinServiceId_devuelve400YNunca500() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get(BASE_URL).param("fecha", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El ID del servicio es obligatorio"));

        verifyNoInteractions(availabilityService);
    }

    @Test
    void sinFecha_devuelve400YNunca500() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get(BASE_URL).param("serviceId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha es obligatoria"));

        verifyNoInteractions(availabilityService);
    }

    @Test
    void fechaMalFormada_devuelve400YNunca500() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get(BASE_URL).param("serviceId", "10").param("fecha", "20/10/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Formato de fecha inválido. Use yyyy-MM-dd (ej. 2026-10-20)"));

        verifyNoInteractions(availabilityService);
    }

    @Test
    void serviceIdNoNumerico_devuelve400YNunca500() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get(BASE_URL).param("serviceId", "abc").param("fecha", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'serviceId' tiene un formato inválido"));

        verifyNoInteractions(availabilityService);
    }

    @Test
    void fechaPasada_devuelve400() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(availabilityService.getAvailability(eq(10L), eq(LocalDate.of(2026, 10, 10))))
                .thenThrow(new IllegalArgumentException("La fecha no puede ser en el pasado"));

        mockMvc.perform(get(BASE_URL).param("serviceId", "10").param("fecha", "2026-10-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha no puede ser en el pasado"));
    }

    @Test
    void peticionValida_devuelve200() throws Exception {
        MockMvc mockMvc = mockMvc();
        AvailabilityResponse response = AvailabilityResponse.builder()
                .fecha(LocalDate.of(2026, 10, 20))
                .idServicio(10L)
                .duracionMinutos(60)
                .slots(List.of(AvailabilitySlotResponse.builder()
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(10, 0))
                        .estado(EstadoSlot.LIBRE)
                        .build()))
                .build();

        when(availabilityService.getAvailability(eq(10L), eq(LocalDate.of(2026, 10, 20))))
                .thenReturn(response);

        mockMvc.perform(get(BASE_URL).param("serviceId", "10").param("fecha", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-10-20"))
                .andExpect(jsonPath("$.idServicio").value(10))
                .andExpect(jsonPath("$.slots[0].estado").value("LIBRE"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(availabilityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
