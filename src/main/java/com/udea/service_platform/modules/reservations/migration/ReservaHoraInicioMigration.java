package com.udea.service_platform.modules.reservations.migration;

import com.udea.service_platform.modules.core.config.TimeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * HU-14: migra las bases creadas antes del cambio de Reserva (columna
 * fecha_inicio time NOT NULL) a la columna hora_inicio (timestamptz).
 * <p>
 * Corre al arrancar, despues de crear todos los singletons: el
 * EntityManagerFactory ya aplico ddl-auto=update (hora_inicio existe) y
 * Tomcat aun no acepta peticiones. Es idempotente: solo rellena hora_inicio
 * nulas y solo quita el NOT NULL de fecha_inicio si sigue puesto. No borra
 * la columna ni datos. En una base nueva o ya migrada no hace nada.
 * <p>
 * Las reservas antiguas no guardan fecha de creacion: por decision del PO se
 * usa la fecha en que corre la migracion (America/Bogota) y la hora de
 * fecha_inicio se interpreta como hora local de Bogota.
 */
@Slf4j
@Component
public class ReservaHoraInicioMigration implements SmartInitializingSingleton {

    private static final String TABLA = "Reservas";
    private static final String COLUMNA_LEGACY = "fecha_inicio";
    private static final String COLUMNA_NUEVA = "hora_inicio";

    private static final String SQL_NULABILIDAD = "SELECT is_nullable FROM information_schema.columns "
            + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?";
    private static final String SQL_RELLENAR = "UPDATE \"Reservas\" "
            + "SET hora_inicio = (CAST(? AS date) + fecha_inicio) AT TIME ZONE ? "
            + "WHERE hora_inicio IS NULL AND fecha_inicio IS NOT NULL";
    private static final String SQL_QUITAR_NOT_NULL =
            "ALTER TABLE \"Reservas\" ALTER COLUMN fecha_inicio DROP NOT NULL";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ReservaHoraInicioMigration(DataSource dataSource, Clock clock) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.clock = clock;
    }

    @Override
    public void afterSingletonsInstantiated() {
        migrate();
    }

    public Resultado migrate() {
        Optional<String> nulabilidadLegacy = nulabilidad(COLUMNA_LEGACY);
        if (nulabilidadLegacy.isEmpty()) {
            log.debug("Migracion hora_inicio: \"Reservas\" no tiene fecha_inicio, nada que migrar");
            return Resultado.SIN_CAMBIOS;
        }
        if (nulabilidad(COLUMNA_NUEVA).isEmpty()) {
            log.warn("Migracion hora_inicio: \"Reservas\" tiene fecha_inicio pero no hora_inicio, se omite");
            return Resultado.SIN_CAMBIOS;
        }

        LocalDate fechaMigracion = LocalDate.now(clock.withZone(TimeConfig.ZONA_BOGOTA));
        boolean quitarNotNull = "NO".equals(nulabilidadLegacy.get());

        Integer filas = transactionTemplate.execute(status -> {
            int actualizadas = jdbcTemplate.update(SQL_RELLENAR, fechaMigracion, TimeConfig.ZONA_BOGOTA.getId());
            if (quitarNotNull) {
                jdbcTemplate.execute(SQL_QUITAR_NOT_NULL);
            }
            return actualizadas;
        });
        int filasRellenadas = filas == null ? 0 : filas;

        log.info("Migracion hora_inicio: {} reservas rellenadas con la fecha {} ({}); NOT NULL de fecha_inicio retirado: {}",
                filasRellenadas, fechaMigracion, TimeConfig.ZONA_BOGOTA, quitarNotNull);
        return new Resultado(true, filasRellenadas, quitarNotNull);
    }

    private Optional<String> nulabilidad(String columna) {
        List<String> filas = jdbcTemplate.queryForList(SQL_NULABILIDAD, String.class, TABLA, columna);
        return filas.stream().findFirst();
    }

    /**
     * @param columnaLegacy    true si la tabla tenia fecha_inicio y hora_inicio
     * @param filasRellenadas  reservas a las que se les asigno hora_inicio
     * @param notNullRetirado  true si se quito el NOT NULL de fecha_inicio
     */
    public record Resultado(boolean columnaLegacy, int filasRellenadas, boolean notNullRetirado) {
        public static final Resultado SIN_CAMBIOS = new Resultado(false, 0, false);
    }
}
