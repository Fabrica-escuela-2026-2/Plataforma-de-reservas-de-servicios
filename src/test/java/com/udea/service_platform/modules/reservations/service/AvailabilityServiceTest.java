package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.dto.BloqueoFranja;
import com.udea.service_platform.modules.services.dto.HorarioFranja;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.service.ScheduleLookupService;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Long SERVICE_ID = 10L;
    private static final Long PROVIDER_ID = 5L;
    private static final int DURACION = 60;

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final Clock CLOCK_FIJO = Clock.fixed(
            HOY.atTime(8, 0).atZone(BOGOTA).toInstant(), BOGOTA);

    @Mock
    private ServiceLookupService serviceLookupService;

    @Mock
    private ScheduleLookupService scheduleLookupService;

    @Mock
    private ReservaRepository reservaRepository;

    private AvailabilityServiceImpl availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityServiceImpl(
                serviceLookupService, scheduleLookupService, reservaRepository, CLOCK_FIJO);
    }

    @Test
    void sinHorarioEseDia_devuelveSlotsVacio() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(), List.of(), List.of(), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertTrue(response.getSlots().isEmpty());
        assertEquals(DURACION, response.getDuracionMinutos());
        assertEquals(SERVICE_ID, response.getIdServicio());
        assertEquals(fecha, response.getFecha());
    }

    @Test
    void variosTramos_generaFranjasPorTramo() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 11), franja(fecha, 14, 16)), List.of(), List.of(), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(4, response.getSlots().size());
        assertEquals(LocalTime.of(9, 0), response.getSlots().get(0).getHoraInicio());
        assertEquals(LocalTime.of(10, 0), response.getSlots().get(0).getHoraFin());
        assertEquals(LocalTime.of(15, 0), response.getSlots().get(3).getHoraInicio());
    }

    @Test
    void duracionQueNoDivideElHorario_truncaSinFranjaIncompleta() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 11)), List.of(), List.of(), 50);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(2, response.getSlots().size());
        assertEquals(LocalTime.of(9, 0), response.getSlots().get(0).getHoraInicio());
        assertEquals(LocalTime.of(9, 50), response.getSlots().get(0).getHoraFin());
        assertEquals(LocalTime.of(9, 50), response.getSlots().get(1).getHoraInicio());
        assertEquals(LocalTime.of(10, 40), response.getSlots().get(1).getHoraFin());
    }

    @Test
    void bloqueoDiaCompleto_marcaTodasBloqueadas() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 11)),
                List.of(new BloqueoFranja(null, null)), List.of(), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(2, response.getSlots().size());
        assertTrue(response.getSlots().stream().allMatch(s -> s.getEstado() == EstadoSlot.BLOQUEADA));
    }

    @Test
    void bloqueoParcial_marcaSoloLasFranjasQueSolapa() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 12)),
                List.of(new BloqueoFranja(LocalTime.of(10, 0), LocalTime.of(11, 0))), List.of(), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.LIBRE, response.getSlots().get(0).getEstado());
        assertEquals(EstadoSlot.BLOQUEADA, response.getSlots().get(1).getEstado());
        assertEquals(EstadoSlot.LIBRE, response.getSlots().get(2).getEstado());
    }

    @Test
    void bloqueadaPrimaSobreReserva() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 11)),
                List.of(new BloqueoFranja(LocalTime.of(9, 30), LocalTime.of(10, 30))),
                List.of(reserva(fecha, 9, 0, DURACION)), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.BLOQUEADA, response.getSlots().get(0).getEstado());
    }

    @Test
    void reservaActivaSolapada_marcaOcupada() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 11)), List.of(),
                List.of(reserva(fecha, 9, 30, DURACION)), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.OCUPADA, response.getSlots().get(0).getEstado());
        assertEquals(EstadoSlot.OCUPADA, response.getSlots().get(1).getEstado());
    }

    @Test
    void reservaCanceladaLiberaElSlot() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 10)), List.of(),
                List.of(reservaConEstado(fecha, 9, 0, DURACION, EstadoReserva.CANCELADA)), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.LIBRE, response.getSlots().get(0).getEstado());
    }

    @Test
    void reservaDeOtroDiaSeIgnora() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 10)), List.of(),
                List.of(reserva(HOY, 9, 0, DURACION)), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.LIBRE, response.getSlots().get(0).getEstado());
    }

    @Test
    void reservaSinDuracionAcordadaUsaDuracionDelServicio() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 11)), List.of(),
                List.of(reserva(fecha, 10, 0, null)), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.OCUPADA, response.getSlots().get(1).getEstado());
    }

    @Test
    void reservaConHoraInicioNulaSeIgnora() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, 9, 10)), List.of(),
                List.of(reservaConHoraNula(fecha)), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.LIBRE, response.getSlots().get(0).getEstado());
    }

    @Test
    void hoyOmiteFranjasYaIniciadas() {
        stubLookup(HOY, List.of(franja(HOY, 7, 10)), List.of(), List.of(), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, HOY);

        assertEquals(2, response.getSlots().size());
        assertEquals(LocalTime.of(8, 0), response.getSlots().get(0).getHoraInicio());
        assertEquals(LocalTime.of(9, 0), response.getSlots().get(1).getHoraInicio());
    }

    @Test
    void fechaPasada_lanzaError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> availabilityService.getAvailability(SERVICE_ID, HOY.minusDays(1)));

        assertEquals("La fecha no puede ser en el pasado", ex.getMessage());
    }

    @Test
    void servicioConDuracionCero_lanzaError() {
        LocalDate fecha = HOY.plusDays(1);
        when(serviceLookupService.findActiveService(SERVICE_ID)).thenReturn(servicio(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> availabilityService.getAvailability(SERVICE_ID, fecha));

        assertEquals("La duración del servicio debe ser mayor a cero", ex.getMessage());
    }

    @Test
    void horarioDeOtroDiaSeIgnora() {
        LocalDate fecha = HOY.plusDays(1);
        HorarioFranja otroDia = new HorarioFranja(fecha.getDayOfWeek().getValue() % 7 + 1,
                LocalTime.of(9, 0), LocalTime.of(11, 0));
        stubLookup(fecha, List.of(otroDia), List.of(), List.of(), DURACION);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertTrue(response.getSlots().isEmpty());
    }

    @Test
    void horarioQueCruzaMedianoche_generaFranjasHastaElDiaSiguiente() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, LocalTime.of(23, 0), LocalTime.of(0, 0))),
                List.of(), List.of(), 30);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(2, response.getSlots().size());
        assertEquals(LocalTime.of(23, 0), response.getSlots().get(0).getHoraInicio());
        assertEquals(LocalTime.of(23, 30), response.getSlots().get(0).getHoraFin());
        assertEquals(LocalTime.of(23, 30), response.getSlots().get(1).getHoraInicio());
        assertEquals(LocalTime.of(0, 0), response.getSlots().get(1).getHoraFin());
    }

    @Test
    void bloqueoQueCruzaMedianoche_marcaSoloLaFranjaPosterior() {
        LocalDate fecha = HOY.plusDays(1);
        stubLookup(fecha, List.of(franja(fecha, LocalTime.of(23, 0), LocalTime.of(0, 0))),
                List.of(new BloqueoFranja(LocalTime.of(23, 30), LocalTime.of(0, 0))),
                List.of(), 30);

        AvailabilityResponse response = availabilityService.getAvailability(SERVICE_ID, fecha);

        assertEquals(EstadoSlot.LIBRE, response.getSlots().get(0).getEstado());
        assertEquals(EstadoSlot.BLOQUEADA, response.getSlots().get(1).getEstado());
    }

    @Test
    void reservaDeMedianocheBogota_seCuentaEnElDiaLocalCorrecto() {
        LocalDate fecha = HOY.plusDays(1);
        Instant instante = fecha.atTime(23, 30).atZone(BOGOTA).toInstant();

        // 23:30 en Bogota (UTC-5) es el dia siguiente en UTC.
        assertEquals(Instant.parse("2026-10-22T04:30:00Z"), instante);

        stubLookup(fecha, List.of(franja(fecha, LocalTime.of(23, 0), LocalTime.of(23, 59))), List.of(),
                List.of(reservaConInstante(instante, 20)), 20);

        availabilityService.getAvailability(SERVICE_ID, fecha);

        // El rango UTC pedido al repositorio es el dia local de Bogota y contiene la reserva.
        ArgumentCaptor<Instant> inicio = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> fin = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(reservaRepository).findActiveReservationsInRange(
                eq(SERVICE_ID), eq(EstadoReserva.ACTIVA.getId()), inicio.capture(), fin.capture());

        assertEquals(fecha.atStartOfDay(BOGOTA).toInstant(), inicio.getValue());
        assertEquals(fecha.plusDays(1).atStartOfDay(BOGOTA).toInstant(), fin.getValue());
        assertFalse(instante.isBefore(inicio.getValue()));
        assertTrue(instante.isBefore(fin.getValue()));
    }

    private void stubLookup(LocalDate fecha, List<HorarioFranja> franjas, List<BloqueoFranja> bloqueos,
                            List<Reserva> reservas, int duracion) {
        when(serviceLookupService.findActiveService(SERVICE_ID)).thenReturn(servicio(duracion));
        when(scheduleLookupService.findServiceSchedules(SERVICE_ID)).thenReturn(franjas);
        when(scheduleLookupService.findProviderBlocks(PROVIDER_ID, fecha)).thenReturn(bloqueos);
        when(reservaRepository.findActiveReservationsInRange(
                eq(SERVICE_ID), eq(EstadoReserva.ACTIVA.getId()), any(), any()))
                .thenReturn(reservas);
    }

    private HorarioFranja franja(LocalDate fecha, int inicio, int fin) {
        return franja(fecha, LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
    }

    private HorarioFranja franja(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return new HorarioFranja(fecha.getDayOfWeek().getValue(), inicio, fin);
    }

    private Reserva reserva(LocalDate fecha, int hora, int minuto, Integer duracion) {
        Instant instante = fecha.atTime(hora, minuto).atZone(BOGOTA).toInstant();
        return reservaConInstante(instante, duracion);
    }

    private Reserva reservaConInstante(Instant instante, Integer duracion) {
        return Reserva.builder()
                .idEstado(EstadoReserva.ACTIVA.getId())
                .horaInicio(instante)
                .duracionAcordada(duracion)
                .build();
    }

    private Reserva reservaConEstado(LocalDate fecha, int hora, int minuto, Integer duracion,
                                     EstadoReserva estado) {
        Reserva reserva = reserva(fecha, hora, minuto, duracion);
        reserva.setIdEstado(estado.getId());
        return reserva;
    }

    private Reserva reservaConHoraNula(LocalDate fecha) {
        return Reserva.builder()
                .idEstado(EstadoReserva.ACTIVA.getId())
                .horaInicio(null)
                .build();
    }

    private Service servicio(int duracion) {
        return Service.builder()
                .id(SERVICE_ID)
                .nombre("Corte")
                .duracion(duracion)
                .idProveedor(PROVIDER_ID)
                .activo(true)
                .build();
    }
}
