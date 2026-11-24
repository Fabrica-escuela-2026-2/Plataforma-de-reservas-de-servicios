package com.udea.service_platform.modules.resources.service;

import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.core.exception.ResourceNotFoundException;
import com.udea.service_platform.modules.resources.dto.RecursoRequest;
import com.udea.service_platform.modules.resources.dto.RecursoResponse;
import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecursoServiceImplTest {

    private static final Long PROVEEDOR = 9701L;
    private static final Long ADMIN = 9703L;
    private static final Long ID_RECURSO = 9711L;
    private static final Instant AHORA = Instant.parse("2026-11-22T20:00:00Z");

    @Mock
    private RecursoRepository recursoRepository;

    @Mock
    private LinkedServicesProvider linkedServicesProvider;

    @Mock
    private ResourceReservationGuard resourceReservationGuard;

    private RecursoServiceImpl recursoService;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        recursoService = new RecursoServiceImpl(recursoRepository, linkedServicesProvider,
                resourceReservationGuard, reloj);
    }

    private RecursoRequest request(String nombre, Long idProveedor) {
        RecursoRequest request = new RecursoRequest();
        request.setNombre(nombre);
        request.setIdProveedor(idProveedor);
        return request;
    }

    private Recurso recurso(Long id, Long idProveedor, boolean activo) {
        return Recurso.builder().idRecursos(id).nombre("Taladro").idProveedor(idProveedor).activo(activo).build();
    }

    @Test
    void crear_recortaElNombreYUsaElProveedorDelToken() {
        when(recursoRepository.existsByIdProveedorAndNombreIgnoreCase(PROVEEDOR, "Taladro")).thenReturn(false);
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        RecursoResponse respuesta = recursoService.crear(request("  Taladro  ", ADMIN), PROVEEDOR, false);

        ArgumentCaptor<Recurso> captor = ArgumentCaptor.forClass(Recurso.class);
        verify(recursoRepository).save(captor.capture());
        assertEquals("Taladro", captor.getValue().getNombre());
        assertEquals(PROVEEDOR, captor.getValue().getIdProveedor());
        assertTrue(captor.getValue().getActivo());
        assertEquals("Taladro", respuesta.getNombre());
        assertEquals(PROVEEDOR, respuesta.getIdProveedor());
        assertTrue(respuesta.getActivo());
        assertTrue(respuesta.getServicios().isEmpty());
    }

    @Test
    void crear_nombreVacio_lanza400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.crear(request("   ", null), PROVEEDOR, false));

        assertEquals("El nombre del recurso es obligatorio", ex.getMessage());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void crear_nombreDuplicadoDelMismoProveedor_lanza409() {
        when(recursoRepository.existsByIdProveedorAndNombreIgnoreCase(PROVEEDOR, "Taladro")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> recursoService.crear(request("Taladro", null), PROVEEDOR, false));

        assertEquals("Ya existe un recurso con ese nombre", ex.getMessage());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void crear_adminConProveedor_usaElProveedorDelBody() {
        when(recursoRepository.existsByIdProveedorAndNombreIgnoreCase(ADMIN, "Taladro")).thenReturn(false);
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        RecursoResponse respuesta = recursoService.crear(request("Taladro", ADMIN), PROVEEDOR, true);

        assertEquals(ADMIN, respuesta.getIdProveedor());
    }

    @Test
    void crear_adminSinProveedor_lanza400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.crear(request("Taladro", null), PROVEEDOR, true));

        assertEquals("Debes indicar el proveedor", ex.getMessage());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void crear_nombreNull_lanza400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.crear(request(null, null), PROVEEDOR, false));

        assertEquals("El nombre del recurso es obligatorio", ex.getMessage());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void listar_sinFiltro_usaElRepositorioGeneral() {
        when(recursoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(recurso(9711L, PROVEEDOR, true)));
        when(linkedServicesProvider.findLinkedServices(List.of(9711L))).thenReturn(Map.of());

        List<RecursoResponse> respuesta = recursoService.listar(null);

        assertEquals(1, respuesta.size());
        verify(linkedServicesProvider).findLinkedServices(List.of(9711L));
    }

    @Test
    void listar_conFiltro_usaElRepositorioDelProveedor() {
        when(recursoRepository.findByIdProveedorOrderByNombreAsc(PROVEEDOR))
                .thenReturn(List.of(recurso(9711L, PROVEEDOR, true)));
        when(linkedServicesProvider.findLinkedServices(List.of(9711L))).thenReturn(Map.of());

        List<RecursoResponse> respuesta = recursoService.listar(PROVEEDOR);

        assertEquals(1, respuesta.size());
        verify(linkedServicesProvider).findLinkedServices(List.of(9711L));
    }

    @Test
    void misRecursos_cargaLosServiciosEnUnaSolaLlamada() {
        when(recursoRepository.findByIdProveedorOrderByNombreAsc(PROVEEDOR))
                .thenReturn(List.of(recurso(9711L, PROVEEDOR, true), recurso(9712L, PROVEEDOR, false)));
        when(linkedServicesProvider.findLinkedServices(List.of(9711L, 9712L)))
                .thenReturn(Map.of(9711L, List.of(new LinkedService(9801L, "Servicio A", true))));

        List<RecursoResponse> respuesta = recursoService.misRecursos(PROVEEDOR);

        assertEquals(2, respuesta.size());
        assertEquals(1, respuesta.get(0).getServicios().size());
        assertEquals("Servicio A", respuesta.get(0).getServicios().get(0).nombre());
        assertTrue(respuesta.get(1).getServicios().isEmpty());
        assertFalse(respuesta.get(1).getActivo());
        verify(linkedServicesProvider, times(1)).findLinkedServices(List.of(9711L, 9712L));
    }

    @Test
    void inactivar_recursoInexistente_lanza404() {
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(java.util.Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> recursoService.inactivar(ID_RECURSO, PROVEEDOR, false));

        assertEquals("El recurso no existe", ex.getMessage());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void inactivar_recursoAjeno_lanza403() {
        when(recursoRepository.findById(ID_RECURSO))
                .thenReturn(java.util.Optional.of(recurso(ID_RECURSO, ADMIN, true)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> recursoService.inactivar(ID_RECURSO, PROVEEDOR, false));

        assertEquals("El recurso no pertenece al proveedor", ex.getMessage());
        verify(resourceReservationGuard, never()).hasFutureOccupyingReservations(any(), any());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void inactivar_adminPuedeInactivarRecursoDeOtroProveedor() {
        Recurso recurso = recurso(ID_RECURSO, PROVEEDOR, true);
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(java.util.Optional.of(recurso));
        when(resourceReservationGuard.hasFutureOccupyingReservations(ID_RECURSO, AHORA)).thenReturn(false);
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        RecursoResponse respuesta = recursoService.inactivar(ID_RECURSO, ADMIN, true);

        assertFalse(respuesta.getActivo());
        verify(recursoRepository).save(recurso);
    }

    @Test
    void inactivar_conReservasFuturas_lanza409YNoGuarda() {
        Recurso recurso = recurso(ID_RECURSO, PROVEEDOR, true);
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(java.util.Optional.of(recurso));
        when(resourceReservationGuard.hasFutureOccupyingReservations(ID_RECURSO, AHORA)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> recursoService.inactivar(ID_RECURSO, PROVEEDOR, false));

        assertEquals("El recurso tiene reservas pendientes. Resuélvelas antes de darlo de baja.", ex.getMessage());
        assertTrue(recurso.getActivo());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }

    @Test
    void inactivar_sinReservasFuturas_guardaInactivo() {
        Recurso recurso = recurso(ID_RECURSO, PROVEEDOR, true);
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(java.util.Optional.of(recurso));
        when(resourceReservationGuard.hasFutureOccupyingReservations(ID_RECURSO, AHORA)).thenReturn(false);
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        RecursoResponse respuesta = recursoService.inactivar(ID_RECURSO, PROVEEDOR, false);

        assertFalse(respuesta.getActivo());
        verify(recursoRepository).save(recurso);
    }

    @Test
    void inactivar_yaInactivo_esIdempotente() {
        Recurso recurso = recurso(ID_RECURSO, PROVEEDOR, false);
        when(recursoRepository.findById(ID_RECURSO)).thenReturn(java.util.Optional.of(recurso));

        RecursoResponse respuesta = recursoService.inactivar(ID_RECURSO, PROVEEDOR, false);

        assertFalse(respuesta.getActivo());
        verify(resourceReservationGuard, never()).hasFutureOccupyingReservations(any(), any());
        verify(recursoRepository, never()).save(any(Recurso.class));
    }
}
