package com.udea.service_platform.modules.resources.migration;

import com.udea.service_platform.modules.reservations.migration.BaseDePruebas;
import com.udea.service_platform.modules.resources.model.Recurso;
import com.udea.service_platform.modules.resources.repository.RecursoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-20: la base real tiene en "Recursos" columnas obligatorias que la
 * aplicacion no llena (descripcion, tipo). El arranque no debe fallar al
 * repartir los recursos compartidos, y despues se pueden crear recursos nuevos.
 */
@SpringBootTest
class RecursoMigrationColumnasObligatoriasIntegrationTest {

    private static final String BASE = "recursos_columnas_obligatorias";
    private static final long PROVEEDOR_UNO = 9901L;
    private static final long PROVEEDOR_DOS = 9902L;
    private static String url;

    @DynamicPropertySource
    static void baseConColumnasDeBd(DynamicPropertyRegistry registry) {
        url = BaseDePruebas.crearBase(BASE);
        BaseDePruebas.aplicarEsquemaDeMain(url, "recursos-esquema-bd.sql");
        BaseDePruebas.ejecutar(url, "INSERT INTO \"Recursos\" (id_recurso, descripcion, tipo) "
                + "VALUES (1, 'Sala', 'SALA')");
        insertarServicio(201L, PROVEEDOR_UNO, 1L);
        insertarServicio(202L, PROVEEDOR_DOS, 1L);
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> BaseDePruebas.USER);
        registry.add("spring.datasource.password", () -> BaseDePruebas.PASSWORD);
    }

    private static void insertarServicio(long id, long proveedor, long recurso) {
        BaseDePruebas.ejecutar(url, "INSERT INTO \"Servicios\" "
                + "(id_servicio, activo, categoria, duracion, usuario_proveedor, id_recursos, nombre, precio) "
                + "VALUES (" + id + ", true, 'QA', 30, " + proveedor + ", " + recurso
                + ", 'Servicio " + id + "', 25000)");
    }

    @Autowired
    private RecursoRepository recursoRepository;

    @Test
    void alArrancar_quitaElNotNullDeLasColumnasAjenasYReparteElRecursoCompartido() {
        List<String> obligatorias = BaseDePruebas.consultar(url, "SELECT column_name || '|' || is_nullable "
                + "FROM information_schema.columns WHERE table_name = 'Recursos' "
                + "AND column_name IN ('descripcion', 'tipo') ORDER BY column_name");
        assertEquals(List.of("descripcion|YES", "tipo|YES"), obligatorias);

        Recurso principal = recursoRepository.findById(1L).orElseThrow();
        assertEquals(PROVEEDOR_UNO, principal.getIdProveedor());

        List<String> servicios = BaseDePruebas.consultar(url, "SELECT id_recursos::text FROM \"Servicios\" "
                + "WHERE id_servicio = 202");
        long idReplica = Long.parseLong(servicios.get(0));
        assertTrue(idReplica > 1L);
        assertEquals(PROVEEDOR_DOS, recursoRepository.findById(idReplica).orElseThrow().getIdProveedor());
    }

    @Test
    void despuesDeMigrar_sePuedeCrearUnRecursoSinLasColumnasAjenas() {
        Recurso creado = recursoRepository.save(Recurso.builder()
                .nombre("Recurso nuevo")
                .idProveedor(PROVEEDOR_UNO)
                .build());

        assertNotNull(creado.getIdRecursos());
        assertTrue(recursoRepository.findById(creado.getIdRecursos()).isPresent());
    }
}
