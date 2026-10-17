package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.modules.services.dto.BloqueoRequest;
import com.udea.service_platform.modules.services.dto.BloqueoResponse;
import com.udea.service_platform.modules.services.model.Bloqueo;
import com.udea.service_platform.modules.services.repository.BloqueoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoServiceTest {

    private static final Long PROVIDER_ID = 5L;
    private static final Long OTHER_PROVIDER_ID = 99L;

    @Mock
    private BloqueoRepository bloqueoRepository;

    @InjectMocks
    private BloqueoServiceImpl bloqueoService;

    private BloqueoRequest fullDayRequest(LocalDate fecha, String motivo) {
        return new BloqueoRequest(fecha, null, null, motivo);
    }

    private BloqueoRequest timedRequest(LocalDate fecha, String inicio, String fin, String motivo) {
        return new BloqueoRequest(fecha, LocalTime.parse(inicio), LocalTime.parse(fin), motivo);
    }

    private void stubSaveConId() {
        when(bloqueoRepository.save(any(Bloqueo.class))).thenAnswer(invocation -> {
            Bloqueo bloqueo = invocation.getArgument(0);
            bloqueo.setIdBloqueo(77L);
            return bloqueo;
        });
    }

    @Test
    void createBlock_fullDay_savesWithNullTimes() {
        stubSaveConId();

        BloqueoResponse response = bloqueoService.createBlock(
                fullDayRequest(LocalDate.now().plusDays(3), "Vacaciones"), PROVIDER_ID);

        ArgumentCaptor<Bloqueo> captor = ArgumentCaptor.forClass(Bloqueo.class);
        verify(bloqueoRepository).save(captor.capture());
        assertNull(captor.getValue().getHoraInicio());
        assertNull(captor.getValue().getHoraFin());
        assertEquals(PROVIDER_ID, captor.getValue().getIdUsuarioProveedor());

        assertEquals(77L, response.getIdBloqueo());
        assertNull(response.getHoraInicio());
        assertEquals("Vacaciones", response.getMotivo());
    }

    @Test
    void createBlock_timeRange_savesBothTimes() {
        stubSaveConId();

        BloqueoResponse response = bloqueoService.createBlock(
                timedRequest(LocalDate.now().plusDays(1), "10:00", "14:00", "Cita médica"), PROVIDER_ID);

        ArgumentCaptor<Bloqueo> captor = ArgumentCaptor.forClass(Bloqueo.class);
        verify(bloqueoRepository).save(captor.capture());
        assertEquals(LocalTime.of(10, 0), captor.getValue().getHoraInicio());
        assertEquals(LocalTime.of(14, 0), captor.getValue().getHoraFin());

        assertEquals(LocalTime.of(10, 0), response.getHoraInicio());
        assertEquals(LocalTime.of(14, 0), response.getHoraFin());
    }

    @Test
    void createBlock_pastDate_throwsAndNeverSaves() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueoService.createBlock(
                        fullDayRequest(LocalDate.now().minusDays(1), "Ayuno"), PROVIDER_ID));

        assertEquals("La fecha no puede ser en el pasado", ex.getMessage());
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    void createBlock_today_isAllowed() {
        stubSaveConId();

        BloqueoResponse response = bloqueoService.createBlock(
                fullDayRequest(LocalDate.now(), "Imprevisto del día"), PROVIDER_ID);

        assertEquals(LocalDate.now(), response.getFecha());
    }

    @Test
    void createBlock_onlyHoraInicio_throwsAndNeverSaves() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueoService.createBlock(
                        new BloqueoRequest(LocalDate.now().plusDays(2), LocalTime.of(10, 0), null, "Parcial"),
                        PROVIDER_ID));

        assertEquals("Debe indicar ambas horas (inicio y fin) o ninguna", ex.getMessage());
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    void createBlock_onlyHoraFin_throwsAndNeverSaves() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueoService.createBlock(
                        new BloqueoRequest(LocalDate.now().plusDays(2), null, LocalTime.of(14, 0), "Parcial"),
                        PROVIDER_ID));

        assertEquals("Debe indicar ambas horas (inicio y fin) o ninguna", ex.getMessage());
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    void createBlock_finEqualsInicio_throwsAndNeverSaves() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueoService.createBlock(
                        timedRequest(LocalDate.now().plusDays(2), "10:00", "10:00", "Inválido"), PROVIDER_ID));

        assertEquals("La hora de fin debe ser posterior a la hora de inicio", ex.getMessage());
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    void createBlock_finBeforeInicio_throwsAndNeverSaves() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueoService.createBlock(
                        timedRequest(LocalDate.now().plusDays(2), "14:00", "10:00", "Inválido"), PROVIDER_ID));

        assertEquals("La hora de fin debe ser posterior a la hora de inicio", ex.getMessage());
        verify(bloqueoRepository, never()).save(any(Bloqueo.class));
    }

    @Test
    void createBlock_pastDateAndPartialTimes_reportsDateErrorFirst() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bloqueoService.createBlock(
                        new BloqueoRequest(LocalDate.now().minusDays(1), LocalTime.of(10, 0), null, "Inválido"),
                        PROVIDER_ID));

        assertEquals("La fecha no puede ser en el pasado", ex.getMessage());
    }

    @Test
    void getActiveBlocks_mapsRowsAndQueriesWithToday() {
        when(bloqueoRepository.findByIdUsuarioProveedorAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(PROVIDER_ID), any(LocalDate.class)))
                .thenReturn(List.of(Bloqueo.builder()
                        .idBloqueo(1L)
                        .idUsuarioProveedor(PROVIDER_ID)
                        .fecha(LocalDate.now().plusDays(2))
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(11, 0))
                        .motivo("Viaje")
                        .build()));

        List<BloqueoResponse> responses = bloqueoService.getActiveBlocks(PROVIDER_ID);

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(bloqueoRepository)
                .findByIdUsuarioProveedorAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                        eq(PROVIDER_ID), captor.capture());
        assertFalse(captor.getValue().isBefore(LocalDate.now()));

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getIdBloqueo());
        assertEquals("Viaje", responses.get(0).getMotivo());
    }

    @Test
    void getActiveBlocks_withoutRows_returnsEmptyList() {
        when(bloqueoRepository.findByIdUsuarioProveedorAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                eq(PROVIDER_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        List<BloqueoResponse> responses = bloqueoService.getActiveBlocks(PROVIDER_ID);

        assertTrue(responses.isEmpty());
    }

    @Test
    void deleteBlock_owned_deletes() {
        Bloqueo bloqueo = Bloqueo.builder()
                .idBloqueo(10L)
                .idUsuarioProveedor(PROVIDER_ID)
                .fecha(LocalDate.now().plusDays(1))
                .motivo("Mantenimiento")
                .build();
        when(bloqueoRepository.findByIdBloqueoAndIdUsuarioProveedor(10L, PROVIDER_ID))
                .thenReturn(Optional.of(bloqueo));

        bloqueoService.deleteBlock(10L, PROVIDER_ID);

        verify(bloqueoRepository).delete(bloqueo);
    }

    @Test
    void deleteBlock_missingOrForeign_throwsAccessDeniedAndNeverDeletes() {
        when(bloqueoRepository.findByIdBloqueoAndIdUsuarioProveedor(10L, OTHER_PROVIDER_ID))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> bloqueoService.deleteBlock(10L, OTHER_PROVIDER_ID));

        verify(bloqueoRepository, never()).delete(any(Bloqueo.class));
    }

    @Test
    void createBlock_isTransactionalAndMutating() throws Exception {
        Method method = BloqueoServiceImpl.class.getMethod("createBlock", BloqueoRequest.class, Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void getActiveBlocks_isTransactionalReadOnly() throws Exception {
        Method method = BloqueoServiceImpl.class.getMethod("getActiveBlocks", Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertTrue(transactional.readOnly());
    }

    @Test
    void deleteBlock_isTransactionalAndMutating() throws Exception {
        Method method = BloqueoServiceImpl.class.getMethod("deleteBlock", Long.class, Long.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }
}
