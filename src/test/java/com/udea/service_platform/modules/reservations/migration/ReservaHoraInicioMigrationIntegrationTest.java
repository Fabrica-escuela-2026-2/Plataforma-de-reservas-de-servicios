package com.udea.service_platform.modules.reservations.migration;

import com.udea.service_platform.modules.reservations.model.EstadoReserva;
import com.udea.service_platform.modules.reservations.model.Reserva;
import com.udea.service_platform.modules.reservations.repository.ReservaRepository;
import com.udea.service_platform.modules.services.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-14 / migracion de bases existentes: arranca la aplicacion completa sobre
 * una base con el esquema EXACTO de main (fecha_inicio time NOT NULL) y una
 * reserva antigua a las 10:00. Hibernate (ddl-auto=update) agrega hora_inicio
 * y ReservaHoraInicioMigration debe completar la migracion durante el arranque.
 */
@SpringBootTest
class ReservaHoraInicioMigrationIntegrationTest {

    private static final String BASE = "reservas_migracion_main";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    /** 2026-10-20 12:00 en Bogota: la fecha en que "corre" la migracion. */
    private static final Instant AHORA = Instant.parse("2026-10-20T17:00:00Z");
    /** Fecha de la migracion + 10:00 de Bogota (UTC-5). */
    private static final Instant HORA_ESPERADA = Instant.parse("2026-10-20T15:00:00Z");

    private static String url;
    private static long idServicio;
    private static long idReservaAntigua;

    @DynamicPropertySource
    static void baseCreadaPorMain(DynamicPropertyRegistry registry) {
        url = BaseDePruebas.crearBase(BASE);
        BaseDePruebas.aplicarEsquemaDeMain(url);
        idServicio = BaseDePruebas.insertarDevolviendoId(url, "INSERT INTO \"Servicios\" "
                + "(activo, categoria, duracion, usuario_proveedor, id_recursos, nombre, precio) "
                + "VALUES (true, 'QA', 30, 1, 9001, 'Servicio antiguo', 25000) RETURNING id_servicio");
        idReservaAntigua = BaseDePruebas.insertarDevolviendoId(url, "INSERT INTO \"Reservas\" "
                + "(duracion_acordada, fecha_inicio, estado_reserva, usuario_reserva, valor_acordado, servicio_reserva) "
                + "VALUES (30, '10:00', 1, 2, 25000, " + idServicio + ") RETURNING id_reserva");

        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> BaseDePruebas.USER);
        registry.add("spring.datasource.password", () -> BaseDePruebas.PASSWORD);
    }

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(AHORA, BOGOTA);
        }
    }

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ReservaHoraInicioMigration migration;

    @Test
    void alArrancar_laReservaAntiguaRecibeFechaDeLaMigracionMasSuHoraEnBogota() {
        List<String> filas = BaseDePruebas.consultar(url, "SELECT to_char(hora_inicio AT TIME ZONE 'UTC', "
                + "'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') || '|' || fecha_inicio::text FROM \"Reservas\" "
                + "WHERE id_reserva = " + idReservaAntigua);

        assertEquals(List.of("2026-10-20T15:00:00Z|10:00:00"), filas);
        assertEquals(HORA_ESPERADA, reservaRepository.findById(idReservaAntigua).orElseThrow().getHoraInicio());
    }

    @Test
    void alArrancar_fechaInicioSeConservaPeroAdmiteNull() {
        assertEquals("YES", BaseDePruebas.nulabilidad(url, "fecha_inicio"));
        assertEquals("YES", BaseDePruebas.nulabilidad(url, "hora_inicio"));
    }

    @Test
    void laEntidadPuedeGuardarUnaReservaNueva() {
        Instant horaNueva = Instant.parse("2026-10-28T16:00:00Z");
        Reserva nueva = reservaRepository.saveAndFlush(Reserva.builder()
                .servicio(serviceRepository.findById(idServicio).orElseThrow())
                .idUsuario(2L)
                .idEstado(EstadoReserva.ACTIVA.getId())
                .horaInicio(horaNueva)
                .valorAcordado(BigDecimal.valueOf(25000))
                .duracionAcordada(30)
                .build());

        assertNotNull(nueva.getId());
        List<String> fila = BaseDePruebas.consultar(url, "SELECT to_char(hora_inicio AT TIME ZONE 'UTC', "
                + "'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') || '|' || coalesce(fecha_inicio::text, 'null') "
                + "FROM \"Reservas\" WHERE id_reserva = " + nueva.getId());
        assertEquals(List.of("2026-10-28T16:00:00Z|null"), fila);
    }

    @Test
    void ejecutarLaMigracionOtraVez_noCambiaNada() {
        List<String> antes = BaseDePruebas.filasDeReservas(url, true);

        ReservaHoraInicioMigration.Resultado resultado = migration.migrate();

        assertTrue(resultado.columnaLegacy());
        assertEquals(0, resultado.filasRellenadas());
        assertFalse(resultado.notNullRetirado());
        assertEquals(antes, BaseDePruebas.filasDeReservas(url, true));
        assertEquals("YES", BaseDePruebas.nulabilidad(url, "fecha_inicio"));
    }
}
