package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.core.config.TimeConfig;
import com.udea.service_platform.modules.core.exception.ConflictException;
import com.udea.service_platform.modules.reservations.dto.ReservaRescheduleRequest;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Horario;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.HorarioRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU-17: los avisos de reprogramacion contra PostgreSQL embebido real.
 * No es @Transactional a proposito: el listener corre en BEFORE_COMMIT y solo
 * se dispara si la transaccion realmente se confirma (en una prueba
 * transaccional con rollback nunca se ejecutaria). Si alguien vuelve a
 * AFTER_COMMIT, los avisos se pierden y esta prueba falla con 0 avisos.
 * Cada prueba borra por JDBC lo que creo.
 */
@SpringBootTest
class ReservaRescheduleNotificationIntegrationTest {

    private static final String JDBC_URL = TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres");
    private static final String TIPO = "RESERVA_REPROGRAMADA";

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private User proveedor;
    private User cliente;
    private User otroCliente;
    private Service servicio;
    private LocalDate fecha;
    private Reserva reserva;

    @BeforeEach
    void prepararDatos() {
        jdbc = new JdbcTemplate(dataSource);

        // Fecha futura (el reloj de la aplicacion es el real) con horario 08:00-12:00 ese dia.
        fecha = LocalDate.now(TimeConfig.ZONA_BOGOTA).plusDays(14);

        proveedor = crearUsuario("hu17.proveedor@test.com", "Proveedor de Servicios");
        cliente = crearUsuario("hu17.cliente1@test.com", "Cliente");
        otroCliente = crearUsuario("hu17.cliente2@test.com", "Cliente");

        servicio = serviceRepository.save(Service.builder()
                .nombre("Corte HU17")
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

        reserva = reservaRepository.saveAndFlush(Reserva.builder()
                .idEstado(EstadoReserva.ACTIVA.getId())
                .idUsuario(cliente.getIdUsuario())
                .servicio(servicio)
                .horaInicio(instante(9, 0))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build());
    }

    @AfterEach
    void limpiarDatos() {
        Long idServicio = servicio.getId();
        jdbc.update("DELETE FROM \"Reservas\" WHERE servicio_reserva = ?", idServicio);
        jdbc.update("DELETE FROM \"Notificaciones\" WHERE id_usuario IN (?, ?, ?)",
                proveedor.getIdUsuario(), cliente.getIdUsuario(), otroCliente.getIdUsuario());
        jdbc.update("DELETE FROM \"Horarios\" WHERE servicio_horarios = ?", idServicio.intValue());
        jdbc.update("DELETE FROM \"Servicios\" WHERE id_servicio = ?", idServicio);
        jdbc.update("DELETE FROM \"Usuarios\" WHERE id_usuario IN (?, ?, ?)",
                proveedor.getIdUsuario(), cliente.getIdUsuario(), otroCliente.getIdUsuario());
    }

    @Test
    void reprogramar_guardaUnAvisoParaElClienteYUnoParaElProveedor() {
        reservaService.rescheduleReservation(reserva.getId(), request("10:00"), cliente.getIdUsuario());

        assertEquals(1, contarNotificaciones(cliente.getIdUsuario(), TIPO));
        assertEquals(1, contarNotificaciones(proveedor.getIdUsuario(), TIPO));
    }

    @Test
    void reprogramar_comoProveedor_tambienAvisaAAmbasPartes() {
        reservaService.rescheduleReservation(reserva.getId(), request("11:00"), proveedor.getIdUsuario());

        assertEquals(1, contarNotificaciones(cliente.getIdUsuario(), TIPO));
        assertEquals(1, contarNotificaciones(proveedor.getIdUsuario(), TIPO));
    }

    @Test
    void reprogramar_aUnaFranjaOcupada_noGeneraAvisos() {
        reservaRepository.saveAndFlush(Reserva.builder()
                .idEstado(EstadoReserva.ACTIVA.getId())
                .idUsuario(otroCliente.getIdUsuario())
                .servicio(servicio)
                .horaInicio(instante(10, 0))
                .valorAcordado(new BigDecimal("25000.00"))
                .duracionAcordada(30)
                .build());

        assertThrows(ConflictException.class, () -> reservaService.rescheduleReservation(
                reserva.getId(), request("10:00"), cliente.getIdUsuario()));

        assertEquals(0, contarNotificaciones(cliente.getIdUsuario(), TIPO));
        assertEquals(0, contarNotificaciones(proveedor.getIdUsuario(), TIPO));
        assertEquals(instante(9, 0), reservaRepository.findById(reserva.getId()).orElseThrow().getHoraInicio());
    }

    private ReservaRescheduleRequest request(String hora) {
        return ReservaRescheduleRequest.builder()
                .fecha(fecha.toString())
                .fechaInicio(hora)
                .build();
    }

    private Instant instante(int hora, int minuto) {
        return fecha.atTime(hora, minuto).atZone(TimeConfig.ZONA_BOGOTA).toInstant();
    }

    private User crearUsuario(String correo, String rol) {
        Role role = roleRepository.findAll().stream()
                .filter(r -> rol.equals(r.getNombre()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(Role.builder().nombre(rol).build()));
        return userRepository.save(User.builder()
                .nombre("Test")
                .apellido("HU17")
                .correo(correo)
                .password("hash")
                .role(role)
                .build());
    }

    private int contarNotificaciones(Long idUsuario, String tipo) {
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"Notificaciones\" WHERE id_usuario = ? AND tipo = ?",
                Integer.class, idUsuario, tipo);
        return total == null ? 0 : total;
    }
}