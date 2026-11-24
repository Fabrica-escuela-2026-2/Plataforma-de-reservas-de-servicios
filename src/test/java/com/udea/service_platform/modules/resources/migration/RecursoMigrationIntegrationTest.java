package com.udea.service_platform.modules.resources.migration;

import com.udea.service_platform.modules.reservations.migration.BaseDePruebas;
import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-20 / migracion de bases existentes: arranca la aplicacion completa sobre
 * una base con el esquema EXACTO de main anterior a HU-20 ("Recursos" solo con
 * id_recurso). Hibernate (ddl-auto=update) agrega las columnas nuevas y
 * RecursoMigration debe repartir los recursos compartidos por proveedor,
 * completar nombres y duenos y convertir la llave en identidad.
 */
@SpringBootTest
class RecursoMigrationIntegrationTest {

    private static final String BASE = "recursos_migracion_main";
    private static final long PROVEEDOR_UNO = 9701L;
    private static final long PROVEEDOR_DOS = 9702L;
    private static final long RECURSO_SIN_FILA_COMPARTIDO = 9750L;
    private static final long PROVEEDOR_MENOR = 9810L;
    private static final long PROVEEDOR_MAYOR = 9811L;
    private static final long SERVICIO_PROVEEDOR_MENOR = 9820L;
    private static final long SERVICIO_PROVEEDOR_MAYOR = 9821L;
    private static String url;

    @DynamicPropertySource
    static void baseCreadaPorMain(DynamicPropertyRegistry registry) {
        url = BaseDePruebas.crearBase(BASE);
        BaseDePruebas.aplicarEsquemaDeMain(url, "recursos-esquema-main.sql");
        insertarDatosLegacy();
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> BaseDePruebas.USER);
        registry.add("spring.datasource.password", () -> BaseDePruebas.PASSWORD);
    }

    private static void insertarDatosLegacy() {
        BaseDePruebas.ejecutar(url, "INSERT INTO \"Recursos\" (id_recurso) VALUES (1), (2), (3), (4)");
        insertarServicio(101L, PROVEEDOR_UNO, 1L);
        insertarServicio(102L, PROVEEDOR_DOS, 2L);
        insertarServicio(103L, PROVEEDOR_UNO, 3L);
        insertarServicio(104L, PROVEEDOR_DOS, 3L);
        insertarServicio(105L, PROVEEDOR_UNO, 99L);
        insertarServicio(SERVICIO_PROVEEDOR_MENOR, PROVEEDOR_MENOR, RECURSO_SIN_FILA_COMPARTIDO);
        insertarServicio(SERVICIO_PROVEEDOR_MAYOR, PROVEEDOR_MAYOR, RECURSO_SIN_FILA_COMPARTIDO);
    }

    private static void insertarServicio(long id, long proveedor, long recurso) {
        BaseDePruebas.ejecutar(url, "INSERT INTO \"Servicios\" "
                + "(id_servicio, activo, categoria, duracion, usuario_proveedor, id_recursos, nombre, precio) "
                + "VALUES (" + id + ", true, 'QA', 30, " + proveedor + ", " + recurso
                + ", 'Servicio " + id + "', 25000)");
    }

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private RecursoMigration migration;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static List<String> estadoDeRecursos() {
        return BaseDePruebas.consultar(url, "SELECT id_recurso || '|' || coalesce(nombre, 'null') || '|' || "
                + "coalesce(id_proveedor::text, 'null') || '|' || coalesce(activo::text, 'null') "
                + "FROM \"Recursos\" ORDER BY id_recurso");
    }

    private static List<String> estadoDeServicios() {
        return BaseDePruebas.consultar(url, "SELECT id_servicio || '|' || id_recursos "
                + "FROM \"Servicios\" ORDER BY id_servicio");
    }

    @Test
    void alArrancar_losRecursosDeUnSoloProveedorRecibenNombreYDueno() {
        Recurso uno = recursoRepository.findById(1L).orElseThrow();
        Recurso dos = recursoRepository.findById(2L).orElseThrow();

        assertEquals(PROVEEDOR_UNO, uno.getIdProveedor());
        assertEquals("Recurso 1", uno.getNombre());
        assertTrue(uno.getActivo());
        assertEquals(PROVEEDOR_DOS, dos.getIdProveedor());
        assertEquals("Recurso 2", dos.getNombre());
    }

    @Test
    void alArrancar_unRecursoCompartidoSeReplicaPorProveedorYSeRepuntanLosServicios() {
        Recurso compartido = recursoRepository.findById(3L).orElseThrow();
        assertEquals(PROVEEDOR_UNO, compartido.getIdProveedor());
        assertEquals("Recurso 3", compartido.getNombre());

        List<String> servicios = BaseDePruebas.consultar(url, "SELECT id_servicio || '|' || id_recursos || '|' || "
                + "nombre FROM \"Servicios\" WHERE id_servicio IN (103, 104) ORDER BY id_servicio");
        assertEquals("103|3|Servicio 103", servicios.get(0));

        String[] filaReapuntada = servicios.get(1).split("\\|");
        Long nuevoId = Long.valueOf(filaReapuntada[1]);
        assertTrue(nuevoId > 3L);
        assertEquals("104|" + nuevoId + "|Servicio 104", servicios.get(1));

        Recurso replica = recursoRepository.findById(nuevoId).orElseThrow();
        assertEquals(PROVEEDOR_DOS, replica.getIdProveedor());
        assertEquals("Recurso 3 (proveedor 9702)", replica.getNombre());
    }

    @Test
    void unIdSinFilaCompartidoPorDosProveedores_seRepartePorProveedor() {
        Recurso principal = recursoRepository.findById(RECURSO_SIN_FILA_COMPARTIDO).orElseThrow();
        assertEquals(PROVEEDOR_MENOR, principal.getIdProveedor());
        assertEquals("Recurso 9750", principal.getNombre());
        assertTrue(principal.getActivo());

        List<String> servicios = BaseDePruebas.consultar(url, "SELECT id_servicio || '|' || id_recursos "
                + "FROM \"Servicios\" WHERE id_servicio IN (9820, 9821) ORDER BY id_servicio");
        assertEquals("9820|9750", servicios.get(0));

        Long nuevoId = Long.valueOf(servicios.get(1).split("\\|")[1]);
        assertTrue(nuevoId > RECURSO_SIN_FILA_COMPARTIDO);
        assertEquals("9821|" + nuevoId, servicios.get(1));

        Recurso replica = recursoRepository.findById(nuevoId).orElseThrow();
        assertEquals(PROVEEDOR_MAYOR, replica.getIdProveedor());
        assertEquals("Recurso 9750 (proveedor 9811)", replica.getNombre());
    }

    @Test
    void alArrancar_elIdSinFilaSeCreaYLaFilaSinServiciosQuedaInactiva() {
        Recurso sinFila = recursoRepository.findById(99L).orElseThrow();
        assertEquals(PROVEEDOR_UNO, sinFila.getIdProveedor());
        assertEquals("Recurso 99", sinFila.getNombre());
        assertEquals(List.of("99"), BaseDePruebas.consultar(url,
                "SELECT id_recursos::text FROM \"Servicios\" WHERE id_servicio = 105"));

        Recurso legacy = recursoRepository.findById(4L).orElseThrow();
        assertEquals("Recurso legacy 4", legacy.getNombre());
        assertTrue(legacy.getIdProveedor() == null);
        assertTrue(Boolean.FALSE.equals(legacy.getActivo()));
    }

    @Test
    void despuesDeLaMigracion_unaEntidadNuevaObtieneUnIdMayorQueElMaximo() {
        long maximo = mayorId();

        Recurso guardado = recursoRepository.saveAndFlush(Recurso.builder()
                .nombre("Recurso nuevo")
                .idProveedor(PROVEEDOR_UNO)
                .activo(true)
                .build());

        assertNotNull(guardado.getIdRecursos());
        assertTrue(guardado.getIdRecursos() > maximo);
    }

    private static long mayorId() {
        return Long.parseLong(BaseDePruebas.consultar(url,
                "SELECT max(id_recurso)::text FROM \"Recursos\"").get(0));
    }

    @Test
    void ejecutarLaMigracionOtraVez_noCambiaNada() {
        List<String> recursosAntes = estadoDeRecursos();
        List<String> serviciosAntes = estadoDeServicios();

        RecursoMigration.Resultado resultado = migration.migrate();

        assertFalse(resultado.identidadAgregada());
        assertEquals(0, resultado.recursosCreados());
        assertEquals(0, resultado.serviciosReapuntados());
        assertEquals(0, resultado.filasLegacy());
        assertFalse(resultado.indiceCreado());
        assertEquals(recursosAntes, estadoDeRecursos());
        assertEquals(serviciosAntes, estadoDeServicios());
    }

    @Test
    void laMigracionCreaElIndiceUnicoPorProveedorYNombre() {
        assertEquals(List.of("ux_recursos_proveedor_nombre"), indiceUnico());
    }

    @Test
    void siHayNombresRepetidosPorProveedor_laMigracionOmiteElIndice() {
        jdbcTemplate.execute("DROP INDEX IF EXISTS ux_recursos_proveedor_nombre");
        Long idUno = jdbcTemplate.queryForObject("INSERT INTO \"Recursos\" (nombre, id_proveedor, activo) "
                + "VALUES ('Duplicado', ?, true) RETURNING id_recurso", Long.class, PROVEEDOR_UNO);
        Long idDos = jdbcTemplate.queryForObject("INSERT INTO \"Recursos\" (nombre, id_proveedor, activo) "
                + "VALUES ('duplicado', ?, true) RETURNING id_recurso", Long.class, PROVEEDOR_UNO);
        insertarServicio(106L, PROVEEDOR_UNO, idUno);
        insertarServicio(107L, PROVEEDOR_UNO, idDos);
        try {
            assertFalse(migration.migrate().indiceCreado());
            assertTrue(indiceUnico().isEmpty());
        } finally {
            jdbcTemplate.update("DELETE FROM \"Servicios\" WHERE id_servicio IN (106, 107)");
            jdbcTemplate.update("DELETE FROM \"Recursos\" WHERE id_recurso IN (?, ?)", idUno, idDos);
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_recursos_proveedor_nombre "
                    + "ON \"Recursos\" (id_proveedor, lower(nombre)) "
                    + "WHERE id_proveedor IS NOT NULL AND nombre IS NOT NULL");
            Long maximo = jdbcTemplate.queryForObject("SELECT COALESCE(max(id_recurso), 0) FROM \"Recursos\"",
                    Long.class);
            jdbcTemplate.execute("ALTER TABLE \"Recursos\" ALTER COLUMN id_recurso RESTART WITH " + (maximo + 1));
        }
    }

    private static List<String> indiceUnico() {
        return BaseDePruebas.consultar(url, "SELECT indexname FROM pg_indexes WHERE schemaname = current_schema() "
                + "AND tablename = 'Recursos' AND indexname = 'ux_recursos_proveedor_nombre'");
    }

    @Test
    void siLaColumnaPierdeLaIdentidad_laMigracionLaRestauraConElSiguienteIdLibre() {
        BaseDePruebas.ejecutar(url, "ALTER TABLE \"Recursos\" ALTER COLUMN id_recurso DROP IDENTITY IF EXISTS");
        assertEquals(List.of("NO"), BaseDePruebas.consultar(url,
                "SELECT is_identity FROM information_schema.columns WHERE table_schema = current_schema() "
                        + "AND table_name = 'Recursos' AND column_name = 'id_recurso'"));

        assertTrue(migration.migrate().identidadAgregada());

        assertEquals(List.of("YES"), BaseDePruebas.consultar(url,
                "SELECT is_identity FROM information_schema.columns WHERE table_schema = current_schema() "
                        + "AND table_name = 'Recursos' AND column_name = 'id_recurso'"));
        long maximo = mayorId();
        Recurso guardado = recursoRepository.saveAndFlush(Recurso.builder()
                .nombre("Recurso tras restaurar identidad")
                .idProveedor(PROVEEDOR_DOS)
                .activo(true)
                .build());
        assertTrue(guardado.getIdRecursos() > maximo);
    }
}
