package com.udea.service_platform.modules.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.users.dto.LoginRequest;
import com.udea.service_platform.modules.users.model.Role;
import com.udea.service_platform.modules.users.model.User;
import com.udea.service_platform.modules.users.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class LoginInactiveAccountIntegrationTest {

    private static final String PASSWORD = "ClaveSegura123!";
    private static final Long ID_INACTIVA_CLAVE_INCORRECTA = 910001L;
    private static final Long ID_INACTIVA_CLAVE_CORRECTA = 910002L;
    private static final Long ID_ACTIVA_CLAVE_CORRECTA = 910003L;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Role rolCliente;

    @BeforeEach
    void setUp() {
        rolCliente = roleRepository.findAll().stream()
                .filter(role -> "Cliente".equals(role.getNombre()))
                .findFirst()
                .orElseGet(() -> roleRepository.save(Role.builder().nombre("Cliente").build()));
    }

    private void createUser(Long idUsuario, String correo, String estadoCuenta) {
        jdbcTemplate.update(
                "INSERT INTO \"Usuarios\" (id_usuario, nombre, correo, password_hash, estado_cuenta, rol_usuario) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                idUsuario, "Usuario Prueba", correo, passwordEncoder.encode(PASSWORD), estadoCuenta,
                rolCliente.getIdRol());
    }

    private ResultActions login(String correo, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .correo(correo)
                .password(password)
                .build();
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Test
    void inactiveAccount_withWrongPassword_returnsGenericBadCredentials() throws Exception {
        createUser(ID_INACTIVA_CLAVE_INCORRECTA, "inactiva-clave-incorrecta@test.com", User.ESTADO_INACTIVA);

        login("inactiva-clave-incorrecta@test.com", "ClaveEquivocada123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña inválidos"));
    }

    @Test
    void inactiveAccount_withCorrectPassword_returnsInactiveMessage() throws Exception {
        createUser(ID_INACTIVA_CLAVE_CORRECTA, "inactiva-clave-correcta@test.com", User.ESTADO_INACTIVA);

        login("inactiva-clave-correcta@test.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("La cuenta está inactiva"));
    }

    @Test
    void unknownAccount_returnsGenericBadCredentials() throws Exception {
        login("no-existe@test.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña inválidos"));
    }

    @Test
    void activeAccount_withCorrectPassword_returnsJwt() throws Exception {
        createUser(ID_ACTIVA_CLAVE_CORRECTA, "activa-clave-correcta@test.com", User.ESTADO_ACTIVA);

        login("activa-clave-correcta@test.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isNotEmpty());
    }
}
