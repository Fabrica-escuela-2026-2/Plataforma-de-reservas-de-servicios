package com.udea.service_platform.modules.reservations.repository;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.services.model.Service;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.RoleRepository;
import com.udea.service_platform.modules.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-06: executes the new exists queries against the real schema.
 * Runs against an embedded PostgreSQL (no Docker, no developer database):
 * schema.sql/data.sql provision the fresh instance and test-schema.sql
 * guarantees the lookup tables + reference rows the assertions need.
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ReservaRepositoryTest {

    private static final Instant HORA_INICIO = Instant.parse("2026-10-20T15:00:00Z");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private Role findOrCreateRole(String nombre) {
        return roleRepository.findAll().stream()
                .filter(r -> nombre.equals(r.getNombre()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(Role.builder().nombre(nombre).build()));
    }

    private User createUser(String correo, String rolNombre) {
        User user = User.builder()
                .nombre("Test")
                .correo(correo)
                .password("hash")
                .role(findOrCreateRole(rolNombre))
                .build();
        return userRepository.save(user);
    }

    private Service createService(User proveedor) {
        Service service = Service.builder()
                .nombre("Serv")
                .descripcion("desc")
                .categoria("Cat")
                .duracion(30)
                .precio(new BigDecimal("10.00"))
                .idProveedor(proveedor.getIdUsuario())
                .idRecursos(1L)
                .activo(true)
                .build();
        return serviceRepository.save(service);
    }

    private Reserva createReserva(User cliente, Service service, Integer idEstado) {
        Reserva r = Reserva.builder()
                .servicio(service)
                .idUsuario(cliente.getIdUsuario())
                .horaInicio(HORA_INICIO)
                .idEstado(idEstado)
                .valorAcordado(service.getPrecio())
                .duracionAcordada(service.getDuracion())
                .build();
        return reservaRepository.save(r);
    }

    @Test
    void existsByIdUsuarioAndIdEstado_trueWhenActive() {
        User cliente = createUser("cliente1@test.com", "Cliente");
        User proveedor = createUser("prov1@test.com", "Proveedor de Servicios");
        Service service = createService(proveedor);
        createReserva(cliente, service, EstadoReserva.ACTIVA.getId());

        assertTrue(reservaRepository.existsByIdUsuarioAndIdEstado(cliente.getIdUsuario(),
                EstadoReserva.ACTIVA.getId()));
    }

    @Test
    void existsByIdUsuarioAndIdEstado_falseWhenCancelled() {
        User cliente = createUser("cliente2@test.com", "Cliente");
        User proveedor = createUser("prov2@test.com", "Proveedor de Servicios");
        Service service = createService(proveedor);
        createReserva(cliente, service, EstadoReserva.CANCELADA.getId());

        assertFalse(reservaRepository.existsByIdUsuarioAndIdEstado(cliente.getIdUsuario(),
                EstadoReserva.ACTIVA.getId()));
    }

    @Test
    void existsActiveByProviderId_trueWhenActive() {
        User cliente = createUser("cliente3@test.com", "Cliente");
        User proveedor = createUser("prov3@test.com", "Proveedor de Servicios");
        Service service = createService(proveedor);
        createReserva(cliente, service, EstadoReserva.ACTIVA.getId());

        assertTrue(reservaRepository.existsActiveByProviderId(proveedor.getIdUsuario(),
                EstadoReserva.ACTIVA.getId()));
    }

    @Test
    void existsActiveByProviderId_falseWhenCancelled() {
        User cliente = createUser("cliente4@test.com", "Cliente");
        User proveedor = createUser("prov4@test.com", "Proveedor de Servicios");
        Service service = createService(proveedor);
        createReserva(cliente, service, EstadoReserva.CANCELADA.getId());

        assertFalse(reservaRepository.existsActiveByProviderId(proveedor.getIdUsuario(),
                EstadoReserva.ACTIVA.getId()));
    }
}
