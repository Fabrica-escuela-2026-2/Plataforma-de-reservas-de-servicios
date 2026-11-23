package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.services.model.Bloqueo;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.BloqueoRepository;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import com.udea.service_platform.modules.services.service.ServiceLookupService;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.RoleRepository;
import com.udea.service_platform.modules.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU-15: creacion de reservas contra PostgreSQL embebido real (esquema de
 * Hibernate). No es @Transactional a proposito: la prueba de concurrencia
 * necesita transacciones que realmente se confirmen y compitan por el bloqueo.
 * Cada prueba borra por JDBC lo que creo.
 */
@SpringBootTest
class ReservaCreacionIntegrationTest {

    private static final String JDBC_URL = TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ServiceLookupService serviceLookupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private BloqueoRepository bloqueoRepository;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private User proveedor;
    private User clienteUno;
    private User clienteDos;
    private Service servicio;
    private LocalDate fecha;

    @BeforeEach
    void prepararDatos() {
        jdbc = new JdbcTemplate(dataSource);

        // Una fecha futura (el reloj de la aplicacion es el real) con horario 08:00-12:00 ese dia.
        fecha = LocalDate.now(TimeConfig.ZONA_BOGOTA).plusDays(14);

        proveedor = crearUsuario("hu15.proveedor@test.com", "Proveedor de Servicios");
        clienteUno = crearUsuario("hu15.cliente1@test.com", "Cliente");
        clienteDos = crearUsuario("hu15.cliente2@test.com", "Cliente");

        servicio = serviceRepository.save(Service.builder()
                .nombre("Corte HU15")
                .descripcion("desc")
                .categoria("Cat")
                .duracion(30)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(proveedor.getIdUsuario())
                .idRecursos(1L)
                .activo(true)
                .build());

        horarioRepository.save(Horario.builder()
                .idDiaSemana(fecha.getDayOfWeek().getValue())
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .idServicio(servicio.getId().intValue())
                .build());
    }

    @AfterEach
    void limpiarDatos() {
        Long idServicio = servicio.getId();
        jdbc.update("DELETE FROM \"Reservas\" WHERE servicio_reserva = ?", idServicio);
        jdbc.update("DELETE FROM \"Notificaciones\" WHERE id_usuario IN (?, ?, ?)",
                proveedor.getIdUsuario(), clienteUno.getIdUsuario(), clienteDos.getIdUsuario());
        jdbc.update("DELETE FROM \"Horarios\" WHERE servicio_horarios = ?", idServicio.intValue());
        jdbc.update("DELETE FROM \"Bloqueos\" WHERE id_usuario_proveedor = ?", proveedor.getIdUsuario());
        jdbc.update("DELETE FROM \"Servicios\" WHERE id_servicio = ?", idServicio);
        jdbc.update("DELETE FROM \"Usuarios\" WHERE id_usuario IN (?, ?, ?)",
                proveedor.getIdUsuario(), clienteUno.getIdUsuario(), clienteDos.getIdUsuario());
    }

    @Test
    void reservaExitosa_quedaActiva_conservaPrecioYNotificaAClienteYProveedor() {
        ReservaResponse response = reservaService.createReservation(request("08:00"), clienteUno.getIdUsuario());

        assertEquals("ACTIVA", response.getEstado());
        assertEquals(LocalTime.of(8, 0), response.getFechaInicio());
        assertEquals(fecha, response.getFecha());
        assertEquals(1, contarReservas());

        // el proveedor cambia precio y duracion despues de la reserva
        Service actual = serviceRepository.findById(servicio.getId()).orElseThrow();
        actual.setPrecio(new BigDecimal("40000.00"));
        actual.setDuracion(60);
        serviceRepository.save(actual);

        BigDecimal valorGuardado = jdbc.queryForObject(
                "SELECT valor_acordado FROM \"Reservas\" WHERE servicio_reserva = ?",
                BigDecimal.class, servicio.getId());
        Integer duracionGuardada = jdbc.queryForObject(
                "SELECT duracion_acordada FROM \"Reservas\" WHERE servicio_reserva = ?",
                Integer.class, servicio.getId());
        assertEquals(new BigDecimal("25000.00"), valorGuardado);
        assertEquals(30, duracionGuardada);

        assertEquals(1, contarNotificaciones(clienteUno.getIdUsuario(), "RESERVA_CONFIRMADA"));
        assertEquals(1, contarNotificaciones(proveedor.getIdUsuario(), "NUEVA_RESERVA"));
    }

    @Test
    void horarioYaTomado_segundoClienteRecibeConflictoYNoSeCreaOtraReserva() {
        reservaService.createReservation(request("08:00"), clienteUno.getIdUsuario());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.createReservation(request("08:00"), clienteDos.getIdUsuario()));

        assertEquals("El horario ya no está disponible. Por favor elige otro horario.", ex.getMessage());
        assertEquals(1, contarReservas());
        assertEquals(0, contarNotificaciones(clienteDos.getIdUsuario(), "RESERVA_CONFIRMADA"));
    }

    @Test
    void dosClientesReservanElMismoHorarioAlMismoTiempo_soloUnoLoLogra() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ReservaResponse> primero = pool.submit(reservarCuandoSalga(salida, clienteUno));
            Future<ReservaResponse> segundo = pool.submit(reservarCuandoSalga(salida, clienteDos));
            salida.countDown();

            List<ReservaResponse> exitosas = new ArrayList<>();
            List<Throwable> fallos = new ArrayList<>();
            for (Future<ReservaResponse> futuro : List.of(primero, segundo)) {
                try {
                    exitosas.add(futuro.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    fallos.add(e.getCause());
                }
            }

            assertEquals(1, exitosas.size());
            assertEquals(1, fallos.size());
            assertInstanceOf(ConflictException.class, fallos.get(0));
            assertEquals("El horario ya no está disponible. Por favor elige otro horario.",
                    fallos.get(0).getMessage());
            assertEquals(1, contarReservas());
            // solo la reserva ganadora genera avisos: uno para su cliente y uno para el proveedor
            assertEquals(2, contarNotificacionesDe(proveedor, clienteUno, clienteDos));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void horarioFueraDelHorarioDeAtencion_noSePuedeReservar() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("12:00"), clienteUno.getIdUsuario()));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        assertEquals(0, contarReservas());
        assertEquals(0, contarNotificacionesDe(proveedor, clienteUno, clienteDos));
    }

    @Test
    void horarioBloqueadoPorElProveedor_noSePuedeReservar() {
        bloqueoRepository.save(Bloqueo.builder()
                .idUsuarioProveedor(proveedor.getIdUsuario())
                .fecha(fecha)
                .horaInicio(LocalTime.of(10, 0))
                .horaFin(LocalTime.of(11, 0))
                .motivo("Almuerzo")
                .build());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("10:00"), clienteUno.getIdUsuario()));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        assertEquals(0, contarReservas());
    }

    @Test
    void fechaPasada_noSePuedeReservar() {
        ReservaRequest ayer = ReservaRequest.builder()
                .idServicio(servicio.getId())
                .fecha(LocalDate.now(TimeConfig.ZONA_BOGOTA).minusDays(1).toString())
                .fechaInicio("08:00")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(ayer, clienteUno.getIdUsuario()));

        assertEquals("Ese horario no se puede reservar", ex.getMessage());
        assertEquals(0, contarReservas());
    }

    @Test
    void servicioDesactivadoPorElProveedor_noEstaDisponible() {
        Service actual = serviceRepository.findById(servicio.getId()).orElseThrow();
        actual.setActivo(false);
        serviceRepository.save(actual);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("08:00"), clienteUno.getIdUsuario()));

        assertEquals("El servicio no está disponible", ex.getMessage());
        assertEquals(0, contarReservas());
    }

    @Test
    void cuentaDelProveedorInactiva_elServicioNoEstaDisponible() {
        User actual = userRepository.findById(proveedor.getIdUsuario()).orElseThrow();
        actual.setEstadoCuenta(User.ESTADO_INACTIVA);
        userRepository.save(actual);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(request("08:00"), clienteUno.getIdUsuario()));

        assertEquals("El servicio no está disponible", ex.getMessage());
        assertEquals(0, contarReservas());
    }

    @Test
    void servicioInexistente_noEstaDisponible() {
        ReservaRequest inexistente = ReservaRequest.builder()
                .idServicio(Long.MAX_VALUE)
                .fecha(fecha.toString())
                .fechaInicio("08:00")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.createReservation(inexistente, clienteUno.getIdUsuario()));

        assertEquals("El servicio no está disponible", ex.getMessage());
    }

    @Test
    void bloquearElServicioExigeUnaTransaccionActiva() {
        assertThrows(IllegalTransactionStateException.class,
                () -> serviceLookupService.lockActiveService(servicio.getId()));
    }

    private Callable<ReservaResponse> reservarCuandoSalga(CountDownLatch salida, User cliente) {
        return () -> {
            salida.await();
            return reservaService.createReservation(request("09:00"), cliente.getIdUsuario());
        };
    }

    private ReservaRequest request(String hora) {
        return ReservaRequest.builder()
                .idServicio(servicio.getId())
                .fecha(fecha.toString())
                .fechaInicio(hora)
                .build();
    }

    private User crearUsuario(String correo, String rol) {
        Role role = roleRepository.findAll().stream()
                .filter(r -> rol.equals(r.getNombre()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(Role.builder().nombre(rol).build()));
        return userRepository.save(User.builder()
                .nombre("Test")
                .apellido("HU15")
                .correo(correo)
                .password("hash")
                .role(role)
                .build());
    }

    private int contarReservas() {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Reservas\" WHERE servicio_reserva = ?", Integer.class, servicio.getId());
        return total == null ? 0 : total;
    }

    private int contarNotificaciones(Long idUsuario, String tipo) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Notificaciones\" WHERE id_usuario = ? AND tipo = ?",
                Integer.class, idUsuario, tipo);
        return total == null ? 0 : total;
    }

    private int contarNotificacionesDe(User uno, User dos, User tres) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Notificaciones\" WHERE id_usuario IN (?, ?, ?)",
                Integer.class, uno.getIdUsuario(), dos.getIdUsuario(), tres.getIdUsuario());
        return total == null ? 0 : total;
    }
}
