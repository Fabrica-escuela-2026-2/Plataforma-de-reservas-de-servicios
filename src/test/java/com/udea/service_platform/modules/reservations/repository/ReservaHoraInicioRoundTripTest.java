package com.udea.service_platform.modules.reservations.repository;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.reservations.model.Reserva;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-14 / migracion a hora_inicio: prueba de ida y vuelta contra la base
 * embebida. Reproduce la conversion de produccion (la columna pasa a
 * timestamptz tratando los valores previos como UTC), verifica el tipo con
 * information_schema y comprueba que un Instant guardado se lee igual desde
 * America/Bogota y desde Pacific/Auckland, siempre con conexion nueva.
 */
@SpringBootTest
class ReservaHoraInicioRoundTripTest {

    private static final String JDBC_URL = TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres");
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final ZoneId AUCKLAND = ZoneId.of("Pacific/Auckland");

    private static final Instant INSTANTE = Instant.parse("2026-10-20T19:30:00Z");

    private static final long SERVICE_ID = 9101L;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> JDBC_URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @Autowired
    private ReservaRepository reservaRepository;

    private TimeZone zonaOriginal;

    @BeforeEach
    void aplicarConversionDeProduccionYRecordarZona() throws Exception {
        zonaOriginal = TimeZone.getDefault();

        try (Connection connection = abrirConexion(); Statement statement = connection.createStatement()) {
            // Columna legacy (timestamp sin zona) y conversion exacta de produccion.
            statement.execute("ALTER TABLE \"Reservas\" ALTER COLUMN hora_inicio TYPE timestamp "
                    + "USING hora_inicio AT TIME ZONE 'UTC'");
            statement.execute("ALTER TABLE \"Reservas\" ALTER COLUMN hora_inicio TYPE timestamptz "
                    + "USING hora_inicio AT TIME ZONE 'UTC'");
            statement.execute("INSERT INTO \"Servicios\" "
                    + "(id_servicio, nombre, categoria, duracion, precio, usuario_proveedor, id_recursos, activo) "
                    + "VALUES (" + SERVICE_ID + ", 'Servicio round trip', 'Cat', 30, 1000, 556, 1, true) "
                    + "ON CONFLICT (id_servicio) DO NOTHING");
        }
    }

    @AfterEach
    void restaurarZona() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    void columnaEsTimestampWithTimeZone() throws Exception {
        try (Connection connection = abrirConexion(); Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(
                    "SELECT data_type FROM information_schema.columns "
                            + "WHERE table_name = 'Reservas' AND column_name = 'hora_inicio'");
            rs.next();
            assertEquals("timestamp with time zone", rs.getString("data_type"));
        }
    }

    @Test
    void instantGuardadoEnBogota_seLeeIgualEnAuckland() throws Exception {
        Long id = guardarEn(BOGOTA);

        assertEquals(INSTANTE, leerEn(AUCKLAND, id));
    }

    @Test
    void instantGuardadoEnAuckland_seLeeIgualEnBogota() throws Exception {
        Long id = guardarEn(AUCKLAND);

        assertEquals(INSTANTE, leerEn(BOGOTA, id));
    }

    @Test
    void valorGuardadoQuedaEnUtcEnLaBase() throws Exception {
        Long id = guardarEn(BOGOTA);

        try (Connection connection = abrirConexion(); Statement statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE 'UTC'");
            ResultSet rs = statement.executeQuery(
                    "SELECT hora_inicio FROM \"Reservas\" WHERE id_reserva = " + id);
            rs.next();
            assertEquals(Timestamp.from(INSTANTE), rs.getTimestamp("hora_inicio"));
        }
    }

    @Test
    void entidadJpaLeeElMismoInstantQueLaColumna() throws Exception {
        Long id = guardarEn(BOGOTA);

        TimeZone.setDefault(TimeZone.getTimeZone(AUCKLAND));

        Reserva reserva = reservaRepository.findById(id).orElseThrow();

        assertEquals(INSTANTE, reserva.getHoraInicio());
    }

    private Long guardarEn(ZoneId zona) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(zona));

        try (Connection connection = abrirConexion()) {
            connection.createStatement().execute("SET TIME ZONE '" + zona.getId() + "'");
            var ps = connection.prepareStatement(
                    "INSERT INTO \"Reservas\" (estado_reserva, servicio_reserva, usuario_reserva, hora_inicio) "
                            + "VALUES (1, " + SERVICE_ID + ", 1, ?) RETURNING id_reserva");
            ps.setTimestamp(1, Timestamp.from(INSTANTE));
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    private Instant leerEn(ZoneId zona, Long id) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone(zona));

        try (Connection connection = abrirConexion()) {
            connection.createStatement().execute("SET TIME ZONE '" + zona.getId() + "'");
            var ps = connection.prepareStatement(
                    "SELECT hora_inicio FROM \"Reservas\" WHERE id_reserva = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getTimestamp("hora_inicio").toInstant();
        }
    }

    private Connection abrirConexion() throws Exception {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }
}
