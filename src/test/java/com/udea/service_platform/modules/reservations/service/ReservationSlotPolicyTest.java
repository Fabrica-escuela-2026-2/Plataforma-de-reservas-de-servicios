package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.service.ScheduleLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationSlotPolicyTest {

    private static final Long SERVICE_ID = 9908L;
    private static final Long RECURSO_ID = 9907L;
    private static final Long PROVIDER_ID = 9901L;

    private final LocalDate fecha = LocalDate.now(TimeConfig.ZONA_BOGOTA).plusDays(7);

    @Mock
    private ScheduleLookupService scheduleLookupService;

    @Mock
    private ReservaRepository reservaRepository;

    private ReservationSlotPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ReservationSlotPolicy(scheduleLookupService, reservaRepository);
    }

    @Test
    void evaluate_sinAforo_normalizaAUnoYDevuelveUnCupo() {
        SlotDecision decision = policy.evaluate(contexto(null, List.of(), List.of()),
                LocalTime.of(10, 0), 30);

        assertEquals(EstadoSlot.LIBRE, decision.estado());
        assertEquals(SlotReason.OK, decision.reason());
        assertEquals(1, decision.cuposTotales());
        assertEquals(1, decision.cuposDisponibles());
        assertNull(decision.mensaje());
    }

    @Test
    void evaluate_conAforoTresYDosReservasSolapadas_dejaUnCupoLibre() {
        SlotContext contexto = contexto(3,
                List.of(reserva(1L, EstadoReserva.ACTIVA, 10, 0, 60),
                        reserva(2L, EstadoReserva.CONFIRMADA, 10, 30, 60)),
                List.of());

        SlotDecision decision = policy.evaluate(contexto, LocalTime.of(10, 45), 30);

        assertEquals(EstadoSlot.LIBRE, decision.estado());
        assertEquals(3, decision.cuposTotales());
        assertEquals(1, decision.cuposDisponibles());
        assertNull(decision.mensaje());
    }

    @Test
    void evaluate_conAforoDosYDosReservasSolapadas_agotaLosCupos() {
        SlotContext contexto = contexto(2,
                List.of(reserva(1L, EstadoReserva.ACTIVA, 10, 0, 60),
                        reserva(2L, EstadoReserva.CONFIRMADA, 10, 30, 60)),
                List.of());

        SlotDecision decision = policy.evaluate(contexto, LocalTime.of(10, 45), 30);

        assertEquals(EstadoSlot.OCUPADA, decision.estado());
        assertEquals(SlotReason.CAPACITY_FULL, decision.reason());
        assertEquals(2, decision.cuposTotales());
        assertEquals(0, decision.cuposDisponibles());
        assertEquals(ReservationSlotPolicy.MSG_SIN_CUPOS, decision.mensaje());
    }

    @Test
    void evaluate_conReservaCancelada_noLaCuentaComoCupoOcupado() {
        SlotContext contexto = contexto(1,
                List.of(reserva(1L, EstadoReserva.CANCELADA, 10, 0, 60)), List.of());

        SlotDecision decision = policy.evaluate(contexto, LocalTime.of(10, 15), 30);

        assertEquals(EstadoSlot.LIBRE, decision.estado());
        assertEquals(1, decision.cuposDisponibles());
    }

    @Test
    void evaluate_conReservaDeOtroServicioEnElMismoRecurso_muestraRecursoOcupado() {
        SlotContext contexto = contexto(5, List.of(),
                List.of(reserva(20L, EstadoReserva.ACTIVA, 10, 0, 60)));

        SlotDecision decision = policy.evaluate(contexto, LocalTime.of(10, 45), 30);

        assertEquals(EstadoSlot.OCUPADA, decision.estado());
        assertEquals(SlotReason.RESOURCE_BUSY, decision.reason());
        assertEquals(5, decision.cuposTotales());
        assertEquals(0, decision.cuposDisponibles());
        assertEquals(ReservationSlotPolicy.MSG_RECURSO_OCUPADO, decision.mensaje());
    }

    @Test
    void evaluate_fueraDelHorario_priorizaFueraDeHorarioSobreCupos() {
        SlotContext contexto = contexto(1,
                List.of(reserva(1L, EstadoReserva.ACTIVA, 10, 0, 60)),
                List.of(reserva(20L, EstadoReserva.ACTIVA, 10, 0, 60)));

        SlotDecision decision = policy.evaluate(contexto, LocalTime.of(13, 0), 30);

        assertEquals(EstadoSlot.BLOQUEADA, decision.estado());
        assertEquals(SlotReason.OUT_OF_SCHEDULE, decision.reason());
        assertNull(decision.mensaje());
    }

    @Test
    void evaluate_conBloqueoDelProveedor_devuelveBloqueada() {
        SlotContext contexto = contexto(1, List.of(), List.of());
        SlotContext bloqueado = new SlotContext(contexto.serviceId(), contexto.idRecurso(),
                contexto.aforo(), contexto.providerId(), contexto.fecha(), contexto.duracionServicio(),
                contexto.schedule(), List.of(new BloqueoFranja(LocalTime.of(10, 0), LocalTime.of(11, 0))),
                contexto.sameServiceReservations(), contexto.otherServiceReservations(), null);

        SlotDecision decision = policy.evaluate(bloqueado, LocalTime.of(10, 30), 30);

        assertEquals(EstadoSlot.BLOQUEADA, decision.estado());
        assertEquals(SlotReason.PROVIDER_BLOCK, decision.reason());
    }

    @Test
    void evaluate_excluyeLaReservaQueSeEstaReprogramando() {
        SlotContext contexto = new SlotContext(SERVICE_ID, RECURSO_ID, 1, PROVIDER_ID, fecha, 60,
                schedule(), List.of(),
                List.of(reserva(100L, EstadoReserva.ACTIVA, 10, 0, 60)), List.of(), 100L);

        SlotDecision decision = policy.evaluate(contexto, LocalTime.of(10, 30), 30);

        assertEquals(EstadoSlot.LIBRE, decision.estado());
    }

    @Test
    void loadForMutation_cargaVentanaAmpliaYReservasDelRecurso() {
        when(scheduleLookupService.findServiceSchedules(SERVICE_ID)).thenReturn(schedule());
        when(scheduleLookupService.findProviderBlocks(PROVIDER_ID, fecha)).thenReturn(List.of());
        when(reservaRepository.findOccupyingReservationsInRange(eq(SERVICE_ID), any(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.findOccupyingReservationsByRecursoInRange(eq(RECURSO_ID), eq(SERVICE_ID),
                any(), any(), any())).thenReturn(List.of());

        SlotContext contexto = policy.loadForMutation(
                SERVICE_ID, RECURSO_ID, 2, PROVIDER_ID, fecha, 60, 100L);

        assertEquals(100L, contexto.excludeReservationId());
        assertEquals(2, contexto.aforo());
        verify(reservaRepository).findOccupyingReservationsByRecursoInRange(
                eq(RECURSO_ID), eq(SERVICE_ID), any(), any(), any());
    }

    @Test
    void peakConcurrentOccupyingReservations_cuentaElMaximoSolapamiento() {
        Instant desde = fecha.atTime(9, 0).atZone(TimeConfig.ZONA_BOGOTA).toInstant();
        when(reservaRepository.findFutureOccupyingReservations(eq(SERVICE_ID), any(), eq(desde)))
                .thenReturn(List.of(
                        reserva(1L, EstadoReserva.ACTIVA, 10, 0, 60),
                        reserva(2L, EstadoReserva.CONFIRMADA, 10, 30, 60),
                        reserva(3L, EstadoReserva.CANCELADA, 10, 45, 60)));

        assertEquals(2, policy.peakConcurrentOccupyingReservations(SERVICE_ID, 60, desde));
    }

    @Test
    void normalizarAforo_valoresNulosOMenoresAUno_devuelveUno() {
        assertEquals(1, policy.normalizarAforo(null));
        assertEquals(1, policy.normalizarAforo(0));
        assertEquals(1, policy.normalizarAforo(-3));
        assertEquals(4, policy.normalizarAforo(4));
    }

    private SlotContext contexto(Integer aforo, List<Reserva> mismoServicio,
                                 List<Reserva> otroServicio) {
        return new SlotContext(SERVICE_ID, RECURSO_ID, aforo, PROVIDER_ID, fecha, 60,
                schedule(), List.of(), mismoServicio, otroServicio, null);
    }

    private List<HorarioFranja> schedule() {
        return List.of(new HorarioFranja(fecha.getDayOfWeek().getValue(),
                LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    private Reserva reserva(Long id, EstadoReserva estado, int hora, int minuto, Integer duracion) {
        return Reserva.builder()
                .id(id)
                .idEstado(estado.getId())
                .horaInicio(fecha.atTime(hora, minuto).atZone(TimeConfig.ZONA_BOGOTA).toInstant())
                .duracionAcordada(duracion)
                .build();
    }
}
