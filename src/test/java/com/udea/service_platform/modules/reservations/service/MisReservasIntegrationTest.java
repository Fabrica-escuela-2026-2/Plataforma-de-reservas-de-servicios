package com.udea.service_platform.modules.reservations.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.reservations.dto.MiReservaResponse;
import com.udea.service_platform.modules.reservations.dto.MisReservasResponse;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.model.Service;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-16: "Mis reservas" contra PostgreSQL embebido real. Verifica el filtrado
 * por usuario, el orden, los datos de contacto, el estado y la marca de
 * pendiente. No es @Transactional: cada prueba borra por JDBC lo que creo.
 */
@SpringBootTest
class MisReservasIntegrationTest {

    private static final String JDBC_URL = TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private MisReservasService misReservasService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private User proveedorA;
    private User proveedorB;
    private User clienteUno;
    private User clienteDos;
    private User clienteSinReservas;
    private Service servicioA;
    private Service servicioB;

    @BeforeEach
    void prepararDatos() {
        jdbc = new JdbcTemplate(dataSource);

        proveedorA = crearUsuario("hu16.provA@test.com", "Proveedor de Servicios", "Pedro", "Proveedor", "3001111111");
        proveedorB = crearUsuario("hu16.provB@test.com", "Proveedor de Servicios", "Paula", "Otra", "3002222222");
        clienteUno = crearUsuario("hu16.cli1@test.com", "Cliente", "Ana", "Uno", "3111111111");
        clienteDos = crearUsuario("hu16.cli2@test.com", "Cliente", "Beto", "Dos", null);
        clienteSinReservas = crearUsuario("hu16.cli3@test.com", "Cliente", "Carla", "Tres", null);

        servicioA = crearServicio(proveedorA, "Corte A");
        servicioB = crearServicio(proveedorB, "Corte B");

        // clienteUno: pendiente en A (+10d), cancelada en B (+3d), completada en A (-5d), legacy sin hora en A
        guardar(clienteUno, servicioA, EstadoReserva.ACTIVA, dias(10));
        guardar(clienteUno, servicioB, EstadoReserva.CANCELADA, dias(3));
        guardar(clienteUno, servicioA, EstadoReserva.COMPLETADA, dias(-5));
        guardar(clienteUno, servicioA, EstadoReserva.ACTIVA, null);
        // clienteDos: una en A (+2d) y una en B (+1d)
        guardar(clienteDos, servicioA, EstadoReserva.ACTIVA, dias(2));
        guardar(clienteDos, servicioB, EstadoReserva.ACTIVA, dias(1));
    }

    @AfterEach
    void limpiarDatos() {
        jdbc.update("DELETE FROM \"Reservas\" WHERE servicio_reserva IN (?, ?)", servicioA.getId(), servicioB.getId());
        jdbc.update("DELETE FROM \"Servicios\" WHERE id_servicio IN (?, ?)", servicioA.getId(), servicioB.getId());
        jdbc.update("DELETE FROM \"Usuarios\" WHERE id_usuario IN (?, ?, ?, ?, ?)",
                proveedorA.getIdUsuario(), proveedorB.getIdUsuario(), clienteUno.getIdUsuario(),
                clienteDos.getIdUsuario(), clienteSinReservas.getIdUsuario());
    }

    @Test
    void cliente_veSoloSusReservas_ordenadasPorFechaConElContactoDelProveedor() {
        MisReservasResponse response = misReservasService.getMyReservationsAsClient(
                clienteUno.getIdUsuario(), PageRequest.of(0, 10));

        assertNull(response.getMensaje());
        assertEquals(4, response.getTotalElements());
        List<MiReservaResponse> filas = response.getContent();

        // mas reciente primero; la fila antigua sin hora va al final
        assertEquals(EstadoReserva.ACTIVA.getLabel(), filas.get(0).getEstado());
        assertEquals("Corte A", filas.get(0).getServicioNombre());
        assertEquals("CANCELADA", filas.get(1).getEstado());
        assertEquals("COMPLETADA", filas.get(2).getEstado());
        assertNull(filas.get(3).getFecha());

        // contacto del proveedor de cada servicio
        assertEquals("PROVEEDOR", filas.get(0).getContacto().getRol());
        assertEquals("Pedro Proveedor", filas.get(0).getContacto().getNombre());
        assertEquals("hu16.provA@test.com", filas.get(0).getContacto().getCorreo());
        assertEquals("3001111111", filas.get(0).getContacto().getTelefono());
        assertEquals("Paula Otra", filas.get(1).getContacto().getNombre());
        assertEquals("3002222222", filas.get(1).getContacto().getTelefono());
    }

    @Test
    void cliente_distingueLasPendientesDeLasPasadas() {
        List<MiReservaResponse> filas = misReservasService.getMyReservationsAsClient(
                clienteUno.getIdUsuario(), PageRequest.of(0, 10)).getContent();

        assertTrue(filas.get(0).getPendiente());      // confirmada y futura
        assertFalse(filas.get(1).getPendiente());     // cancelada
        assertFalse(filas.get(2).getPendiente());     // completada y pasada
        assertFalse(filas.get(3).getPendiente());     // fila antigua sin hora
    }

    @Test
    void otroCliente_noVeLasReservasDeClienteUno() {
        MisReservasResponse response = misReservasService.getMyReservationsAsClient(
                clienteDos.getIdUsuario(), PageRequest.of(0, 10));

        assertEquals(2, response.getTotalElements());
        assertEquals("Corte B", response.getContent().get(1).getServicioNombre());
        assertEquals("Corte A", response.getContent().get(0).getServicioNombre());
        // clienteDos esta en A (+2d) y en B (+1d): la mas lejana primero
        assertEquals("Pedro Proveedor", response.getContent().get(0).getContacto().getNombre());
        assertEquals("Paula Otra", response.getContent().get(1).getContacto().getNombre());
        assertTrue(response.getContent().stream().allMatch(f -> f.getPendiente()));
    }

    @Test
    void proveedor_veSoloLasReservasDeSusServiciosConElContactoDeCadaCliente() {
        MisReservasResponse response = misReservasService.getMyReservationsAsProvider(
                proveedorA.getIdUsuario(), PageRequest.of(0, 10));

        // A tiene 3 reservas de clienteUno (una sin hora) y 1 de clienteDos
        assertEquals(4, response.getTotalElements());
        assertTrue(response.getContent().stream().allMatch(f -> "Corte A".equals(f.getServicioNombre())));

        MiReservaResponse masReciente = response.getContent().get(0);
        assertEquals("CLIENTE", masReciente.getContacto().getRol());
        assertEquals("Ana Uno", masReciente.getContacto().getNombre());
        assertEquals("hu16.cli1@test.com", masReciente.getContacto().getCorreo());
        assertEquals("3111111111", masReciente.getContacto().getTelefono());

        MiReservaResponse deClienteDos = response.getContent().get(1);
        assertEquals("Beto Dos", deClienteDos.getContacto().getNombre());
        assertNull(deClienteDos.getContacto().getTelefono());
    }

    @Test
    void proveedor_noVeReservasDeServiciosDeOtroProveedor() {
        MisReservasResponse response = misReservasService.getMyReservationsAsProvider(
                proveedorB.getIdUsuario(), PageRequest.of(0, 10));

        assertEquals(2, response.getTotalElements());
        assertTrue(response.getContent().stream().allMatch(f -> "Corte B".equals(f.getServicioNombre())));
    }

    @Test
    void usuarioSinReservas_recibeElMensaje() {
        MisReservasResponse response = misReservasService.getMyReservationsAsClient(
                clienteSinReservas.getIdUsuario(), PageRequest.of(0, 10));

        assertEquals("Todavía no tienes reservas", response.getMensaje());
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void paginacion_devuelveLaPaginaPedida() {
        MisReservasResponse primera = misReservasService.getMyReservationsAsClient(
                clienteUno.getIdUsuario(), PageRequest.of(0, 3));
        MisReservasResponse segunda = misReservasService.getMyReservationsAsClient(
                clienteUno.getIdUsuario(), PageRequest.of(1, 3));

        assertEquals(3, primera.getContent().size());
        assertEquals(1, segunda.getContent().size());
        assertEquals(2, primera.getTotalPages());
        assertEquals(1, segunda.getNumber());
        assertNull(segunda.getMensaje());
    }

    private Instant dias(int cantidad) {
        return Instant.now().plus(cantidad, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
    }

    private Reserva guardar(User cliente, Service servicio, EstadoReserva estado, Instant horaInicio) {
        return reservaRepository.save(Reserva.builder()
                .servicio(servicio)
                .idUsuario(cliente.getIdUsuario())
                .horaInicio(horaInicio)
                .idEstado(estado.getId())
                .valorAcordado(servicio.getPrecio())
                .duracionAcordada(servicio.getDuracion())
                .build());
    }

    private Service crearServicio(User proveedor, String nombre) {
        return serviceRepository.save(Service.builder()
                .nombre(nombre)
                .descripcion("desc")
                .categoria("Cat")
                .duracion(30)
                .precio(new BigDecimal("10.00"))
                .idProveedor(proveedor.getIdUsuario())
                .idRecursos(1L)
                .activo(true)
                .build());
    }

    private User crearUsuario(String correo, String rol, String nombre, String apellido, String telefono) {
        Role role = roleRepository.findAll().stream()
                .filter(r -> rol.equals(r.getNombre()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(Role.builder().nombre(rol).build()));
        return userRepository.save(User.builder()
                .nombre(nombre)
                .apellido(apellido)
                .correo(correo)
                .password("hash")
                .telefono(telefono)
                .role(role)
                .build());
    }
}
