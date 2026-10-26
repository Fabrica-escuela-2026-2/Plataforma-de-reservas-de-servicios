package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.model.Bloqueo;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.repository.BloqueoRepository;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleLookupServiceImplTest {

    private static final Long SERVICE_ID = 10L;
    private static final Long PROVIDER_ID = 5L;
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private BloqueoRepository bloqueoRepository;

    @InjectMocks
    private ScheduleLookupServiceImpl scheduleLookupService;

    @Test
    void findServiceSchedules_mapeaElDiaIsoYLasHoras() {
        when(horarioRepository.findByIdServicio(10)).thenReturn(List.of(
                Horario.builder()
                        .idDiaSemana(3)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .idServicio(10)
                        .build()));

        List<HorarioFranja> franjas = scheduleLookupService.findServiceSchedules(SERVICE_ID);

        assertEquals(1, franjas.size());
        assertEquals(3, franjas.get(0).diaSemana());
        assertEquals(LocalTime.of(9, 0), franjas.get(0).horaInicio());
        assertEquals(LocalTime.of(11, 0), franjas.get(0).horaFin());
    }

    @Test
    void findServiceSchedules_sinHorarios_devuelveListaVacia() {
        when(horarioRepository.findByIdServicio(10)).thenReturn(List.of());

        assertEquals(List.of(), scheduleLookupService.findServiceSchedules(SERVICE_ID));
    }

    @Test
    void findProviderBlocks_mapeaLasHorasDelBloqueo() {
        when(bloqueoRepository.findByIdUsuarioProveedorAndFecha(PROVIDER_ID, FECHA)).thenReturn(List.of(
                Bloqueo.builder()
                        .idUsuarioProveedor(PROVIDER_ID)
                        .fecha(FECHA)
                        .horaInicio(LocalTime.of(10, 0))
                        .horaFin(LocalTime.of(12, 0))
                        .motivo("Mantenimiento")
                        .build()));

        List<BloqueoFranja> bloqueos = scheduleLookupService.findProviderBlocks(PROVIDER_ID, FECHA);

        assertEquals(1, bloqueos.size());
        assertEquals(LocalTime.of(10, 0), bloqueos.get(0).horaInicio());
        assertEquals(LocalTime.of(12, 0), bloqueos.get(0).horaFin());
    }

    @Test
    void findProviderBlocks_bloqueoDeDiaCompleto_sinHoras() {
        when(bloqueoRepository.findByIdUsuarioProveedorAndFecha(PROVIDER_ID, FECHA)).thenReturn(List.of(
                Bloqueo.builder()
                        .idUsuarioProveedor(PROVIDER_ID)
                        .fecha(FECHA)
                        .horaInicio(null)
                        .horaFin(null)
                        .motivo("Vacaciones")
                        .build()));

        List<BloqueoFranja> bloqueos = scheduleLookupService.findProviderBlocks(PROVIDER_ID, FECHA);

        assertEquals(1, bloqueos.size());
        assertNull(bloqueos.get(0).horaInicio());
        assertNull(bloqueos.get(0).horaFin());
        verify(bloqueoRepository).findByIdUsuarioProveedorAndFecha(PROVIDER_ID, FECHA);
    }
}
