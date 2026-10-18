package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.HorarioRequest;
import com.udea.service_platform.modules.services.dto.HorarioResponse;
import com.udea.service_platform.modules.services.model.DiaSemana;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.DiaSemanaRepository;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {

    private static final Long SERVICE_ID = 10L;
    private static final Long OWNER_ID = 5L;
    private static final Long OTHER_PROVIDER_ID = 99L;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private DiaSemanaRepository diaSemanaRepository;

    @InjectMocks
    private HorarioServiceImpl horarioService;

    private Service service;

    @BeforeEach
    void setUp() {
        service = Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte de cabello")
                .categoria("Belleza")
                .duracion(30)
                .precio(new BigDecimal("25.00"))
                .idProveedor(OWNER_ID)
                .idRecursos(1L)
                .build();
    }

    private List<DiaSemana> diasSemana() {
        String[] nombres = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        return IntStream.rangeClosed(1, 7)
                .mapToObj(id -> new DiaSemana(id, nombres[id - 1]))
                .toList();
    }

    private HorarioRequest horario(int dia, String inicio, String fin) {
        return new HorarioRequest(dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    private void stubSaveAllConIds() {
        when(horarioRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Horario> entradas = invocation.getArgument(0);
            AtomicInteger seq = new AtomicInteger(1);
            return entradas.stream()
                    .map(entrada -> Horario.builder()
                            .idHorario(seq.getAndIncrement())
                            .idDiaSemana(entrada.getIdDiaSemana())
                            .horaInicio(entrada.getHoraInicio())
                            .horaFin(entrada.getHoraFin())
                            .idServicio(entrada.getIdServicio())
                            .build())
                    .toList();
        });
    }

    @Test
    void defineSchedule_withValidSlots_replacesScheduleAndReturnsSortedResponses() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());
        stubSaveAllConIds();

        List<HorarioResponse> responses = horarioService.defineSchedule(
                SERVICE_ID,
                List.of(horario(1, "14:00", "16:00"), horario(1, "09:00", "11:00")),
                OWNER_ID);

        verify(horarioRepository).deleteByIdServicio(10);

        ArgumentCaptor<List<Horario>> captor = ArgumentCaptor.forClass(List.class);
        verify(horarioRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().get(0).getIdDiaSemana());
        assertEquals(LocalTime.of(9, 0), captor.getValue().get(0).getHoraInicio());
        assertEquals(LocalTime.of(14, 0), captor.getValue().get(1).getHoraInicio());

        assertEquals(2, responses.size());
        assertEquals("Lunes", responses.get(0).getNombreDia());
        assertEquals(LocalTime.of(9, 0), responses.get(0).getHoraInicio());
        assertEquals(LocalTime.of(11, 0), responses.get(0).getHoraFin());
        assertEquals(SERVICE_ID, responses.get(0).getIdServicio());
        assertNotNull(responses.get(0).getIdHorario());
    }

    @Test
    void defineSchedule_withNullList_throwsAndPersistsNothing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(SERVICE_ID, null, OWNER_ID));

        assertEquals("Debe definir al menos un horario para el servicio", ex.getMessage());
        verifyNoInteractions(serviceRepository, horarioRepository, diaSemanaRepository);
    }

    @Test
    void defineSchedule_withEmptyList_throwsAndPersistsNothing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(SERVICE_ID, List.of(), OWNER_ID));

        assertEquals("Debe definir al menos un horario para el servicio", ex.getMessage());
        verifyNoInteractions(serviceRepository, horarioRepository, diaSemanaRepository);
    }

    @Test
    void defineSchedule_withUnknownService_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(SERVICE_ID, List.of(horario(1, "09:00", "10:00")), OWNER_ID));

        assertEquals("El servicio no existe", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
        verify(horarioRepository, never()).saveAll(anyList());
    }

    @Test
    void defineSchedule_withNonOwnerProvider_throwsAccessDenied() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));

        assertThrows(AccessDeniedException.class,
                () -> horarioService.defineSchedule(SERVICE_ID, List.of(horario(1, "09:00", "10:00")), OTHER_PROVIDER_ID));

        verify(horarioRepository, never()).deleteByIdServicio(10);
        verify(horarioRepository, never()).saveAll(anyList());
    }

    @Test
    void defineSchedule_withUnknownDay_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(SERVICE_ID, List.of(horario(8, "09:00", "10:00")), OWNER_ID));

        assertEquals("El día de la semana no existe", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
        verify(horarioRepository, never()).saveAll(anyList());
    }

    @Test
    void defineSchedule_withNullDay_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(
                        SERVICE_ID, List.of(new HorarioRequest(null, LocalTime.of(9, 0), LocalTime.of(10, 0))), OWNER_ID));

        assertEquals("El día de la semana no existe", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
    }

    @Test
    void defineSchedule_withNullTimes_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(
                        SERVICE_ID, List.of(new HorarioRequest(1, LocalTime.of(9, 0), null)), OWNER_ID));

        assertEquals("La hora de inicio y la hora de fin son obligatorias", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
    }

    @Test
    void defineSchedule_withEqualTimes_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(
                        SERVICE_ID, List.of(horario(1, "09:00", "09:00")), OWNER_ID));

        assertEquals("La hora de fin debe ser posterior a la hora de inicio", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
    }

    @Test
    void defineSchedule_withFinBeforeInicio_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(
                        SERVICE_ID, List.of(horario(1, "14:00", "10:00")), OWNER_ID));

        assertEquals("La hora de fin debe ser posterior a la hora de inicio", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
    }

    @Test
    void defineSchedule_withAdjacentSlotsSameDay_persistsBoth() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());
        stubSaveAllConIds();

        List<HorarioResponse> responses = horarioService.defineSchedule(
                SERVICE_ID,
                List.of(horario(1, "10:00", "12:00"), horario(1, "12:00", "14:00")),
                OWNER_ID);

        verify(horarioRepository).deleteByIdServicio(10);
        assertEquals(2, responses.size());
    }

    @Test
    void defineSchedule_withOverlappingSlotsSameDay_throwsAndPersistsNothing() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(
                        SERVICE_ID,
                        List.of(horario(1, "10:00", "12:00"), horario(1, "11:00", "13:00")),
                        OWNER_ID));

        assertEquals("Los horarios no pueden solaparse en el mismo día", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
        verify(horarioRepository, never()).saveAll(anyList());
    }

    @Test
    void defineSchedule_withSameTimesDifferentDays_persistsBoth() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());
        stubSaveAllConIds();

        List<HorarioResponse> responses = horarioService.defineSchedule(
                SERVICE_ID,
                List.of(horario(1, "10:00", "12:00"), horario(2, "10:00", "12:00")),
                OWNER_ID);

        verify(horarioRepository).deleteByIdServicio(10);
        assertEquals(2, responses.size());
        assertEquals(1, responses.get(0).getIdDiaSemana());
        assertEquals(2, responses.get(1).getIdDiaSemana());
        assertEquals("Martes", responses.get(1).getNombreDia());
    }

    @Test
    void defineSchedule_withOverlapBuriedBetweenLongerSlots_throws() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.defineSchedule(
                        SERVICE_ID,
                        List.of(horario(1, "10:00", "20:00"), horario(1, "11:00", "12:00"), horario(1, "13:00", "14:00")),
                        OWNER_ID));

        assertEquals("Los horarios no pueden solaparse en el mismo día", ex.getMessage());
        verify(horarioRepository, never()).deleteByIdServicio(10);
    }

    @Test
    void getSchedule_withExistingRows_mapsNamesAndOrder() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(diaSemanaRepository.findAll()).thenReturn(diasSemana());
        when(horarioRepository.findByIdServicio(10)).thenReturn(List.of(
                Horario.builder().idHorario(1).idDiaSemana(1).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0)).idServicio(10).build(),
                Horario.builder().idHorario(2).idDiaSemana(2).horaInicio(LocalTime.of(14, 0)).horaFin(LocalTime.of(16, 0)).idServicio(10).build()));

        List<HorarioResponse> responses = horarioService.getSchedule(SERVICE_ID, OWNER_ID);

        assertEquals(2, responses.size());
        assertEquals("Lunes", responses.get(0).getNombreDia());
        assertEquals("Martes", responses.get(1).getNombreDia());
        assertEquals(SERVICE_ID, responses.get(1).getIdServicio());
        assertEquals(2, responses.get(1).getIdHorario());
    }

    @Test
    void getSchedule_withoutRows_returnsEmptyAndSkipsDayLookup() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(horarioRepository.findByIdServicio(10)).thenReturn(List.of());

        List<HorarioResponse> responses = horarioService.getSchedule(SERVICE_ID, OWNER_ID);

        assertTrue(responses.isEmpty());
        verifyNoInteractions(diaSemanaRepository);
    }

    @Test
    void getSchedule_withUnknownService_throws() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> horarioService.getSchedule(SERVICE_ID, OWNER_ID));

        assertEquals("El servicio no existe", ex.getMessage());
        verify(horarioRepository, never()).findByIdServicio(10);
    }

    @Test
    void getSchedule_withNonOwnerProvider_throwsAccessDenied() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));

        assertThrows(AccessDeniedException.class,
                () -> horarioService.getSchedule(SERVICE_ID, OTHER_PROVIDER_ID));

        verify(horarioRepository, never()).findByIdServicio(10);
    }

    @Test
    void defineSchedule_isTransactionalAndMutating() throws Exception {
        Method method = HorarioServiceImpl.class.getMethod("defineSchedule", Long.class, List.class, Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void getSchedule_isTransactionalReadOnly() throws Exception {
        Method method = HorarioServiceImpl.class.getMethod("getSchedule", Long.class, Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }
}
