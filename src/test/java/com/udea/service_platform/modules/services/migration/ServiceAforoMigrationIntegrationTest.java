package com.udea.service_platform.modules.services.migration;

import com.udea.service_platform.modules.reservations.migration.BaseDePruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ServiceAforoMigrationIntegrationTest {

    private static final String BASE = "aforo_migracion_main";
    private static String url;

    @DynamicPropertySource
    static void baseLegacy(DynamicPropertyRegistry registry) {
        url = BaseDePruebas.crearBase(BASE);
        BaseDePruebas.aplicarEsquemaDeMain(url, "reservas-esquema-main.sql");
        BaseDePruebas.ejecutar(url,
                "INSERT INTO \"Servicios\" (id_servicio, activo, categoria, descripcion, duracion, "
                        + "usuario_proveedor, id_recursos, nombre, precio) VALUES "
                        + "(9901, true, 'Belleza', 'Corte', 30, 9901, 9901, 'Corte 9901', 25000), "
                        + "(9902, true, 'Belleza', 'Corte', 45, 9902, 9902, 'Corte 9902', 30000), "
                        + "(9903, false, 'Belleza', 'Corte', 60, 9903, 9903, 'Corte 9903', 35000)");
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> BaseDePruebas.USER);
        registry.add("spring.datasource.password", () -> BaseDePruebas.PASSWORD);
    }

    @Autowired
    private ServiceAforoMigration migration;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reiniciarAforos() {
        jdbcTemplate.update("UPDATE \"Servicios\" SET aforo = 1");
    }

    @Test
    void alArrancar_losServiciosLegacyQuedanConAforoUno() {
        assertEquals(Integer.valueOf(1), aforo(9901L));
        assertEquals(Integer.valueOf(1), aforo(9902L));
        assertEquals(Integer.valueOf(1), aforo(9903L));
    }

    @Test
    void alArrancar_laColumnaQuedaNullableConDefaultUno() {
        String defaultColumna = jdbcTemplate.queryForObject(
                "SELECT column_default FROM information_schema.columns "
                        + "WHERE table_name = 'Servicios' AND column_name = 'aforo'",
                String.class);
        String nullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns "
                        + "WHERE table_name = 'Servicios' AND column_name = 'aforo'",
                String.class);

        assertEquals("1", defaultColumna);
        assertEquals("YES", nullable);
    }

    @Test
    void migrate_llamadaDosVeces_noNormalizaFilasYaCorregidas() {
        ServiceAforoMigration.Resultado resultado = migration.migrate();

        assertEquals(0, resultado.filasNormalizadas());
        assertTrue(resultado.defaultAplicado());
        assertTrue(resultado.columnaNullable());
        assertFalse(resultado.filasNormalizadas() > 0);
    }

    @Test
    void migrate_normalizaValoresInvalidosYConservaLosValidos() {
        jdbcTemplate.update("UPDATE \"Servicios\" SET aforo = 0 WHERE id_servicio = 9901");
        jdbcTemplate.update("UPDATE \"Servicios\" SET aforo = -2 WHERE id_servicio = 9902");
        jdbcTemplate.update("UPDATE \"Servicios\" SET aforo = 5 WHERE id_servicio = 9903");

        ServiceAforoMigration.Resultado resultado = migration.migrate();

        assertEquals(2, resultado.filasNormalizadas());
        assertEquals(Integer.valueOf(1), aforo(9901L));
        assertEquals(Integer.valueOf(1), aforo(9902L));
        assertEquals(Integer.valueOf(5), aforo(9903L));
    }

    private Integer aforo(Long idServicio) {
        return jdbcTemplate.queryForObject(
                "SELECT aforo FROM \"Servicios\" WHERE id_servicio = ?", Integer.class, idServicio);
    }
}
