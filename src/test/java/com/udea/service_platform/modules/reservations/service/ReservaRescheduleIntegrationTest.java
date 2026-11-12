package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.dto.AvailabilityResponse;
import com.udea.service_platform.modules.reservations.dto.EstadoSlot;
import com.udea.service_platform.modules.reservations.dto.ReservaRescheduleRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.reservations.event.ReservaReprogramadaEvent;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Bloqueo;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.BloqueoRepository;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class ReservaRescheduleIntegrationTest {

    private static final ZoneId BOGOTA = TimeConfig.ZONA_BOGOTA;
    private static final Instant AHORA = Instant.parse("2026-11-12T13:00:00Z"); // 08:00 Bogota
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 19);
    private static final Long CLIENT_ID = 2L;
    private static final Long OTHER_CLIENT_ID = 3L;
    private static final Long PROVIDER_ID = 5L;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(AHORA, BOGOTA);
        }
    }

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private BloqueoRepository bloqueoRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    private Service servicio;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        servicio = serviceRepository.save(Service.builder()
                .nombre("Corte")
                .categoria("Cat")
                .duracion(60)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(PROVIDER_ID)
                .idRecursos(1L)
                .activo(true)
                .build());

        horarioRepository.save(Horario.builder()
                .idDiaSemana(FECHA.getDayOfWeek().getValue())
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(18, 0))
                .idServicio(Math.toIntExact(servicio.getId()))
                .build());

        reserva = reservaRepository.saveAndFlush(Reserva.builder()
                .idEstado(EstadoReserva.ACTIVA.getId())
                .idUsuario(CLIENT_ID)
                .servicio(servicio)
                .horaInicio(instante(9, 0))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(60)
                .build());
    }

    @Test
    void reprogramar_actualizaHoraYConservaValorDuracionEstadoYPropietario() {
        ReservaResponse response = reservaService.rescheduleReservation(
                reserva.getId(), request("11:00"), CLIENT_ID);

        Reserva actualizada = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertEquals(instante(11, 0), actualizada.getHoraInicio());
        assertEquals(new BigDecimal("25000.00"), actualizada.getValorAcordado());
        assertEquals(60, actualizada.getDuracionAcordada());
        assertEquals(EstadoReserva.ACTIVA.getId(), actualizada.getIdEstado());
        assertEquals(CLIENT_ID, actualizada.getIdUsuario());
        assertEquals(servicio.getId(), actualizada.getServicio().getId());

        assertEquals(LocalTime.of(11, 0), response.getFechaInicio());
        assertEquals(FECHA, response.getFecha());
        assertEquals("ACTIVA", response.getEstado());
        assertEquals(new BigDecimal("25000.00"), response.getValorAcordado());
        assertEquals(60, response.getDuracionAcordada());
    }

    @Test
    void reprogramar_liberaLaFranjaAnteriorYOcupaLaNueva() {
        reservaService.rescheduleReservation(reserva.getId(), request("11:00"), CLIENT_ID);

        AvailabilityResponse availability = availabilityService.getAvailability(
                servicio.getId(), FECHA);

        assertEquals(EstadoSlot.LIBRE, estadoEn(availability, LocalTime.of(9, 0)));
        assertEquals(EstadoSlot.OCUPADA, estadoEn(availability, LocalTime.of(11, 0)));
    }

    @Test
    void reprogramar_aUnaFranjaOcupada_lanzaConflictoYNoModificaLaReserva() {
        reservaRepository.saveAndFlush(Reserva.builder()
                .idEstado(EstadoReserva.ACTIVA.getId())
                .idUsuario(OTHER_CLIENT_ID)
                .servicio(servicio)
                .horaInicio(instante(11, 0))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(60)
                .build());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.rescheduleReservation(
                        reserva.getId(), request("11:00"), CLIENT_ID));

        assertEquals("El horario seleccionado ya está ocupado", ex.getMessage());
        assertEquals(instante(9, 0), reserva.getHoraInicio());
    }

    @Test
    void reprogramar_aUnaFranjaBloqueada_lanzaConflictoYNoModificaLaReserva() {
        bloqueoRepository.saveAndFlush(Bloqueo.builder()
                .idUsuarioProveedor(PROVIDER_ID)
                .fecha(FECHA)
                .horaInicio(LocalTime.of(11, 0))
                .horaFin(LocalTime.of(12, 0))
                .motivo("Bloqueo de prueba")
                .build());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.rescheduleReservation(
                        reserva.getId(), request("11:00"), CLIENT_ID));

        assertEquals("El horario seleccionado está bloqueado por el proveedor", ex.getMessage());
        assertEquals(instante(9, 0), reserva.getHoraInicio());
    }

    @Test
    void reprogramar_publicaEventoConHoraAnteriorYNueva() {
        reservaService.rescheduleReservation(reserva.getId(), request("11:00"), CLIENT_ID);

        List<ReservaReprogramadaEvent> eventos = applicationEvents
                .stream(ReservaReprogramadaEvent.class)
                .toList();

        assertEquals(1, eventos.size());
        ReservaReprogramadaEvent evento = eventos.get(0);
        assertEquals(reserva.getId(), evento.reservaId());
        assertEquals(CLIENT_ID, evento.actorId());
        assertEquals(CLIENT_ID, evento.clienteId());
        assertEquals(PROVIDER_ID, evento.proveedorId());
        assertEquals(instante(9, 0), evento.horaAnterior());
        assertEquals(instante(11, 0), evento.horaNueva());
    }

    private ReservaRescheduleRequest request(String hora) {
        return ReservaRescheduleRequest.builder()
                .fecha(FECHA.toString())
                .fechaInicio(hora)
                .build();
    }

    private Instant instante(int hora, int minuto) {
        return FECHA.atTime(hora, minuto).atZone(BOGOTA).toInstant();
    }

    private EstadoSlot estadoEn(AvailabilityResponse availability, LocalTime horaInicio) {
        return availability.getSlots().stream()
                .filter(slot -> horaInicio.equals(slot.getHoraInicio()))
                .findFirst()
                .orElseThrow()
                .getEstado();
    }
}
