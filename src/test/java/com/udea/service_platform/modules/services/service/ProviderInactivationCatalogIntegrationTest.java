package com.udea.service_platform.modules.services.service;

import com.udea.service_platform.TestEmbeddedPostgres;
import com.udea.service_platform.modules.services.dto.ServiceResponse;
import com.udea.service_platform.modules.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.udea.service_platform.modules.services.ServiceTestData.insertarProveedor;
import static com.udea.service_platform.modules.services.ServiceTestData.insertarServicio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-06: inactivating a provider account must remove its services from the
 * public catalog, from provider-filtered queries and from availability lookups.
 * Uses the real UserService against embedded PostgreSQL and rolls back.
 */
@SpringBootTest
@Transactional
@Sql(scripts = "/test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ProviderInactivationCatalogIntegrationTest {

    private static final long PROVEEDOR_INACTIVADO = 9501L;
    private static final long PROVEEDOR_ACTIVO = 9502L;
    private static final long SERVICIO_1 = 9601L;
    private static final long SERVICIO_2 = 9602L;
    private static final long SERVICIO_3 = 9603L;
    private static final String CATEGORIA_PRUEBA = "CategoriaIntegracionHU06";

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServiceLookupService serviceLookupService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void prepararDatos() {
        insertarProveedor(jdbcTemplate, PROVEEDOR_INACTIVADO, "proveedor-inactivo-9501@test.com");
        insertarProveedor(jdbcTemplate, PROVEEDOR_ACTIVO, "proveedor-activo-9502@test.com");
        insertarServicio(jdbcTemplate, SERVICIO_1, PROVEEDOR_INACTIVADO, CATEGORIA_PRUEBA, true);
        insertarServicio(jdbcTemplate, SERVICIO_2, PROVEEDOR_INACTIVADO, CATEGORIA_PRUEBA, true);
        insertarServicio(jdbcTemplate, SERVICIO_3, PROVEEDOR_ACTIVO, CATEGORIA_PRUEBA, true);
    }

    @Test
    void alInactivarProveedorElCatalogoSoloMuestraServiciosDelOtroProveedor() {
        assertEquals(3, catalogo(CATEGORIA_PRUEBA, null).size());

        userService.inactivateAccount(PROVEEDOR_INACTIVADO);

        List<ServiceResponse> catalogo = catalogo(CATEGORIA_PRUEBA, null);
        assertEquals(1, catalogo.size());
        assertEquals(SERVICIO_3, catalogo.get(0).getId());
    }

    @Test
    void elFiltroPorProveedorTambienExcluyeLosServiciosDelProveedorInactivado() {
        assertEquals(2, catalogo(CATEGORIA_PRUEBA, PROVEEDOR_INACTIVADO).size());
        assertEquals(1, catalogo(CATEGORIA_PRUEBA, PROVEEDOR_ACTIVO).size());

        userService.inactivateAccount(PROVEEDOR_INACTIVADO);

        assertTrue(catalogo(CATEGORIA_PRUEBA, PROVEEDOR_INACTIVADO).isEmpty());
        assertEquals(1, catalogo(CATEGORIA_PRUEBA, PROVEEDOR_ACTIVO).size());
    }

    @Test
    void elServicioDesactivadoNoSePuedeConsultarEnDisponibilidad() {
        userService.inactivateAccount(PROVEEDOR_INACTIVADO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> serviceLookupService.findActiveService(SERVICIO_1));
        assertEquals("El servicio no está activo", ex.getMessage());
    }

    private List<ServiceResponse> catalogo(String categoria, Long idProveedor) {
        return serviceService.getPublicCatalog(idProveedor, categoria, null, null, PageRequest.of(0, 100))
                .getContent();
    }
}
