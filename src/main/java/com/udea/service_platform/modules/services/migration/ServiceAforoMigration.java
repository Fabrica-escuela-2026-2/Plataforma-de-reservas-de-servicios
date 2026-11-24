package com.udea.service_platform.modules.services.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 * HU-21: agrega el aforo a servicios creados antes de esta historia. Corre al
 * arrancar, despues de ddl-auto, es idempotente y no borra informacion. El
 * valor por defecto se exige en la aplicacion: la columna queda nullable.
 */
@Slf4j
@Component
public class ServiceAforoMigration implements SmartInitializingSingleton {

    static final String TABLA = "Servicios";
    static final String COLUMNA = "aforo";
    static final String MSG_DEFAULT = "1";

    private static final String SQL_ADD =
            "ALTER TABLE \"Servicios\" ADD COLUMN IF NOT EXISTS aforo integer";
    private static final String SQL_FILL =
            "UPDATE \"Servicios\" SET aforo = 1 WHERE aforo IS NULL OR aforo < 1";
    private static final String SQL_DEFAULT =
            "ALTER TABLE \"Servicios\" ALTER COLUMN aforo SET DEFAULT 1";
    private static final String SQL_INFO =
            "SELECT column_default, is_nullable FROM information_schema.columns "
                    + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public ServiceAforoMigration(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public void afterSingletonsInstantiated() {
        migrate();
    }

    public Resultado migrate() {
        Resultado resultado = transactionTemplate.execute(status -> ejecutarMigracion());
        log.info("Migracion aforo: filas normalizadas: {}, default aplicado: {}, columna nullable: {}",
                resultado.filasNormalizadas(), resultado.defaultAplicado(), resultado.columnaNullable());
        return resultado;
    }

    private Resultado ejecutarMigracion() {
        jdbcTemplate.execute(SQL_ADD);
        int filas = jdbcTemplate.update(SQL_FILL);
        jdbcTemplate.execute(SQL_DEFAULT);
        return new Resultado(filas, tieneDefault(), esNullable());
    }

    private boolean tieneDefault() {
        return MSG_DEFAULT.equals(consultarInfo(0));
    }

    private boolean esNullable() {
        return "YES".equals(consultarInfo(1));
    }

    private String consultarInfo(int posicion) {
        List<String[]> filas = jdbcTemplate.query(SQL_INFO,
                (rs, fila) -> new String[]{rs.getString(1), rs.getString(2)}, TABLA, COLUMNA);
        return filas.isEmpty() ? null : filas.get(0)[posicion];
    }

    public record Resultado(int filasNormalizadas, boolean defaultAplicado, boolean columnaNullable) {
    }
}
