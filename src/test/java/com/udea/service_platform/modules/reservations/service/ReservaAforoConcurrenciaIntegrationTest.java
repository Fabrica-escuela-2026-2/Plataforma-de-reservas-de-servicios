package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.dto.ReservaRequest;
import com.udea.service_platform.modules.reservations.dto.ReservaResponse;
import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import com.udea.service_platform.modules.services.service.ServiceService;
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

@SpringBootTest
class ReservaAforoConcurrenciaIntegrationTest {

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
    private ServiceService serviceService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private User proveedor;
    private List<User> clientes;
    private Recurso recurso;
    private Service servicioUno;
    private Service servicioDos;
    private LocalDate fecha;

    @BeforeEach
    void prepararDatos() {
        jdbc = new JdbcTemplate(dataSource);
        reservarIds();
        fecha = LocalDate.now(TimeConfig.ZONA_BOGOTA).plusDays(14);

        proveedor = crearUsuario("hu20.proveedor@test.com", "Proveedor");
        clientes = List.of(
                crearUsuario("hu20.cliente1@test.com", "Cliente"),
                crearUsuario("hu20.cliente2@test.com", "Cliente"),
                crearUsuario("hu20.cliente3@test.com", "Cliente"),
                crearUsuario("hu20.cliente4@test.com", "Cliente"));
        recurso = recursoRepository.save(Recurso.builder()
                .nombre("Silla HU20")
                .idProveedor(proveedor.getIdUsuario())
                .activo(true)
                .build());
        servicioUno = crearServicio("Corte HU20 A", 2);
        servicioDos = crearServicio("Corte HU20 B", 1);
        crearHorario(servicioUno);
        crearHorario(servicioDos);
    }

    @AfterEach
    void limpiarDatos() {
        List<Long> usuarios = new ArrayList<>();
        usuarios.add(proveedor.getIdUsuario());
        clientes.forEach(cliente -> usuarios.add(cliente.getIdUsuario()));
        List<Long> servicios = List.of(servicioUno.getId(), servicioDos.getId());

        jdbc.update("DELETE FROM \"Notificaciones\" WHERE id_usuario IN ("
                + placeholders(usuarios.size()) + ")", usuarios.toArray());
        jdbc.update("DELETE FROM \"Reservas\" WHERE servicio_reserva IN (?, ?)",
                servicioUno.getId(), servicioDos.getId());
        jdbc.update("DELETE FROM \"Horarios\" WHERE servicio_horarios IN (?, ?)",
                servicioUno.getId().intValue(), servicioDos.getId().intValue());
        jdbc.update("DELETE FROM \"Bloqueos\" WHERE id_usuario_proveedor = ?", proveedor.getIdUsuario());
        jdbc.update("DELETE FROM \"Servicios\" WHERE id_servicio IN (?, ?)",
                servicioUno.getId(), servicioDos.getId());
        jdbc.update("DELETE FROM \"Recursos\" WHERE id_recurso = ?", recurso.getIdRecursos());
        jdbc.update("DELETE FROM \"Usuarios\" WHERE id_usuario IN ("
                + placeholders(usuarios.size()) + ")", usuarios.toArray());
    }

    @Test
    void tresClientesConcurrenConAforoDos_soloDosReservan() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Future<ReservaResponse>> futuros = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                futuros.add(pool.submit(reservarCuandoSalga(salida, servicioUno, clientes.get(i))));
            }
            salida.countDown();

            List<ReservaResponse> exitosas = new ArrayList<>();
            List<Throwable> fallos = new ArrayList<>();
            for (Future<ReservaResponse> futuro : futuros) {
                try {
                    exitosas.add(futuro.get(30, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    fallos.add(e.getCause());
                }
            }

            assertEquals(2, exitosas.size());
            assertEquals(1, fallos.size());
            assertInstanceOf(ConflictException.class, fallos.get(0));
            assertEquals(ReservationSlotPolicy.MSG_SIN_CUPOS, fallos.get(0).getMessage());
            assertEquals(2, contarReservas(servicioUno.getId()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void dosServiciosCompartenRecurso_elSegundoServicioRecibeRecursoOcupado() {
        reservaService.createReservation(request(servicioUno.getId(), "09:00"), clientes.get(0).getIdUsuario());
        reservaService.createReservation(request(servicioUno.getId(), "09:00"), clientes.get(1).getIdUsuario());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.createReservation(
                        request(servicioDos.getId(), "09:00"), clientes.get(2).getIdUsuario()));

        assertEquals(ReservationSlotPolicy.MSG_RECURSO_OCUPADO, ex.getMessage());
        assertEquals(0, contarReservas(servicioDos.getId()));
    }

    @Test
    void cancelarUnaReserva_liberaElCupoParaElSiguienteCliente() {
        ReservaResponse primera = reservaService.createReservation(
                request(servicioUno.getId(), "09:00"), clientes.get(0).getIdUsuario());
        reservaService.createReservation(request(servicioUno.getId(), "09:00"), clientes.get(1).getIdUsuario());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> reservaService.createReservation(
                        request(servicioUno.getId(), "09:00"), clientes.get(2).getIdUsuario()));
        assertEquals(ReservationSlotPolicy.MSG_SIN_CUPOS, ex.getMessage());

        reservaService.cancelReservation(primera.getId(), clientes.get(0).getIdUsuario());

        reservaService.createReservation(
                request(servicioUno.getId(), "09:00"), clientes.get(2).getIdUsuario());
        assertEquals(2, contarReservasActivas(servicioUno.getId()));
    }

    @Test
    void patchAforo_picoActualSuperaElNuevoValor_lanzaConflictYConservaElAforo() {
        reservaService.createReservation(request(servicioUno.getId(), "10:00"), clientes.get(0).getIdUsuario());
        reservaService.createReservation(request(servicioUno.getId(), "10:00"), clientes.get(1).getIdUsuario());

        ConflictException ex = assertThrows(ConflictException.class,
                () -> serviceService.updateAforo(servicioUno.getId(), 1, proveedor.getIdUsuario()));

        assertEquals("Ya existen más reservas que el nuevo aforo", ex.getMessage());
        assertEquals(2, serviceRepository.findById(servicioUno.getId()).orElseThrow().getAforo());
    }

    @Test
    void patchAforo_conAforoValido_actualizaElServicio() {
        var respuesta = serviceService.updateAforo(servicioUno.getId(), 3, proveedor.getIdUsuario());

        assertEquals(3, respuesta.getAforo());
        assertEquals(3, serviceRepository.findById(servicioUno.getId()).orElseThrow().getAforo());
    }

    private void reservarIds() {
        jdbc.execute("SELECT setval(pg_get_serial_sequence('\"Usuarios\"', 'id_usuario'), 9900)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('\"Recursos\"', 'id_recurso'), 9900)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('\"Servicios\"', 'id_servicio'), 9900)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('\"Horarios\"', 'id_horario'), 9900)");
        jdbc.execute("SELECT setval(pg_get_serial_sequence('\"Reservas\"', 'id_reserva'), 9900)");
    }

    private User crearUsuario(String correo, String rol) {
        Role role = roleRepository.findAll().stream()
                .filter(r -> rol.equals(r.getNombre()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(Role.builder().nombre(rol).build()));
        return userRepository.save(User.builder()
                .nombre("HU20")
                .apellido("Test")
                .correo(correo)
                .password("hash")
                .role(role)
                .build());
    }

    private Service crearServicio(String nombre, Integer aforo) {
        return serviceRepository.save(Service.builder()
                .nombre(nombre)
                .descripcion("desc")
                .duracion(60)
                .precio(new BigDecimal("25000.00"))
                .idProveedor(proveedor.getIdUsuario())
                .idRecursos(recurso.getIdRecursos())
                .aforo(aforo)
                .activo(true)
                .build());
    }

    private void crearHorario(Service servicio) {
        horarioRepository.save(Horario.builder()
                .idDiaSemana(fecha.getDayOfWeek().getValue())
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .idServicio(servicio.getId().intValue())
                .build());
    }

    private Callable<ReservaResponse> reservarCuandoSalga(CountDownLatch salida,
                                                         Service servicio, User cliente) {
        return () -> {
            salida.await();
            return reservaService.createReservation(
                    request(servicio.getId(), "09:00"), cliente.getIdUsuario());
        };
    }

    private ReservaRequest request(Long idServicio, String hora) {
        return ReservaRequest.builder()
                .idServicio(idServicio)
                .fecha(fecha.toString())
                .fechaInicio(hora)
                .build();
    }

    private String placeholders(int cantidad) {
        return String.join(", ", java.util.Collections.nCopies(cantidad, "?"));
    }

    private int contarReservas(Long idServicio) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Reservas\" WHERE servicio_reserva = ?", Integer.class, idServicio);
        return total == null ? 0 : total;
    }

    private int contarReservasActivas(Long idServicio) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Reservas\" WHERE servicio_reserva = ? AND estado_reserva = 1",
                Integer.class, idServicio);
        return total == null ? 0 : total;
    }
}
