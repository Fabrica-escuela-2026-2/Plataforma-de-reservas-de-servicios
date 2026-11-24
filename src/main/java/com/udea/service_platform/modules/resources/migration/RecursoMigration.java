package com.udea.service_platform.modules.resources.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HU-20: migra las bases creadas antes del alta de recursos. Corre al arrancar,
 * despues de crear todos los singletons: el EntityManagerFactory ya aplico
 * ddl-auto=update y Tomcat aun no acepta peticiones. Es idempotente y no
 * aplica SET NOT NULL: el dato obligatorio se exige en el DTO y el servicio.
 * <p>
 * Un mismo id_recursos podia estar vinculado a servicios de varios
 * proveedores. El primero conserva la fila; cada proveedor adicional recibe
 * una fila nueva y sus servicios se reapuntan.
 */
@Slf4j
@Component
public class RecursoMigration implements SmartInitializingSingleton {

    private static final String TABLA = "Recursos";
    private static final String COLUMNA_ID = "id_recurso";
    private static final String INDICE = "ux_recursos_proveedor_nombre";

    private static final String SQL_INFO_ID = "SELECT is_identity, column_default FROM information_schema.columns "
            + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?";
    private static final String SQL_MAX_ID = "SELECT COALESCE(max(id_recurso), 0) FROM \"Recursos\"";
    private static final String SQL_PROVEEDORES_POR_RECURSO = "SELECT id_recursos, usuario_proveedor "
            + "FROM \"Servicios\" WHERE id_recursos IS NOT NULL "
            + "GROUP BY id_recursos, usuario_proveedor ORDER BY id_recursos, usuario_proveedor";
    private static final String SQL_EXISTE_RECURSO = "SELECT id_recurso FROM \"Recursos\" WHERE id_recurso = ?";
    private static final String SQL_CREAR_CON_ID = "INSERT INTO \"Recursos\" "
            + "(id_recurso, nombre, id_proveedor, activo) VALUES (?, ?, ?, true)";
    private static final String SQL_CREAR_CON_ID_DEVOLVIENDO_ID = "INSERT INTO \"Recursos\" "
            + "(id_recurso, nombre, id_proveedor, activo) VALUES (?, ?, ?, true) RETURNING id_recurso";
    private static final String SQL_COMPLETAR = "UPDATE \"Recursos\" SET nombre = ?, id_proveedor = ? "
            + "WHERE id_recurso = ? AND (nombre IS NULL OR id_proveedor IS NULL)";
    private static final String SQL_REAPUNTAR = "UPDATE \"Servicios\" SET id_recursos = ? "
            + "WHERE id_recursos = ? AND usuario_proveedor = ?";
    private static final String SQL_LEGACY = "UPDATE \"Recursos\" SET activo = false, "
            + "nombre = 'Recurso legacy ' || id_recurso, id_proveedor = NULL "
            + "WHERE id_proveedor IS NULL AND NOT EXISTS (SELECT 1 FROM \"Servicios\" s "
            + "WHERE s.id_recursos = \"Recursos\".id_recurso) "
            + "AND (activo IS DISTINCT FROM false OR nombre IS DISTINCT FROM ('Recurso legacy ' || id_recurso))";
    private static final String SQL_DUPLICADOS = "SELECT lower(nombre) FROM \"Recursos\" "
            + "WHERE id_proveedor IS NOT NULL AND nombre IS NOT NULL "
            + "GROUP BY id_proveedor, lower(nombre) HAVING count(*) > 1";
    private static final String SQL_EXISTE_INDICE = "SELECT indexname FROM pg_indexes "
            + "WHERE schemaname = current_schema() AND tablename = ? AND indexname = ?";
    private static final String SQL_CREAR_INDICE = "CREATE UNIQUE INDEX IF NOT EXISTS " + INDICE
            + " ON \"Recursos\" (id_proveedor, lower(nombre)) "
            + "WHERE id_proveedor IS NOT NULL AND nombre IS NOT NULL";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RecursoMigration(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public void afterSingletonsInstantiated() {
        migrate();
    }

    public Resultado migrate() {
        Resultado resultado = transactionTemplate.execute(status -> ejecutarMigracion());
        log.info("Migracion recursos: identidad agregada: {}, recursos creados: {}, "
                        + "servicios reapuntados: {}, filas legacy: {}, indice creado: {}",
                resultado.identidadAgregada(), resultado.recursosCreados(),
                resultado.serviciosReapuntados(), resultado.filasLegacy(), resultado.indiceCreado());
        return resultado;
    }

    private Resultado ejecutarMigracion() {
        agregarColumnas();
        int[] reparto = repartirRecursos();
        int filasLegacy = marcarRecursosLegacy();
        boolean identidadAgregada = asegurarIdentidad();
        boolean indiceCreado = crearIndiceUnico();
        return new Resultado(identidadAgregada, reparto[0], reparto[1], filasLegacy, indiceCreado);
    }

    private void agregarColumnas() {
        jdbcTemplate.execute("ALTER TABLE \"Recursos\" ADD COLUMN IF NOT EXISTS nombre varchar(120)");
        jdbcTemplate.execute("ALTER TABLE \"Recursos\" ADD COLUMN IF NOT EXISTS id_proveedor bigint");
        jdbcTemplate.execute("ALTER TABLE \"Recursos\" ADD COLUMN IF NOT EXISTS activo boolean DEFAULT true");
        jdbcTemplate.execute("UPDATE \"Recursos\" SET activo = true WHERE activo IS NULL");
    }

    private boolean asegurarIdentidad() {
        List<String[]> info = jdbcTemplate.query(SQL_INFO_ID,
                (rs, fila) -> new String[]{rs.getString(1), rs.getString(2)}, TABLA, COLUMNA_ID);
        if (info.isEmpty()) {
            log.warn("Migracion recursos: no existe la columna id_recurso, se omite la conversion a identidad");
            return false;
        }
        String[] columna = info.get(0);
        if ("YES".equals(columna[0]) || columna[1] != null) {
            return false;
        }
        long siguiente = jdbcTemplate.queryForObject(SQL_MAX_ID, Long.class) + 1L;
        jdbcTemplate.execute("ALTER TABLE \"Recursos\" ALTER COLUMN id_recurso "
                + "ADD GENERATED BY DEFAULT AS IDENTITY (START WITH " + siguiente + ")");
        return true;
    }

    private int[] repartirRecursos() {
        List<String[]> filas = jdbcTemplate.query(SQL_PROVEEDORES_POR_RECURSO,
                (rs, fila) -> new String[]{rs.getString(1), rs.getString(2)});
        Map<Long, List<Long>> proveedoresPorRecurso = new LinkedHashMap<>();
        for (String[] fila : filas) {
            Long idRecurso = Long.valueOf(fila[0]);
            proveedoresPorRecurso.computeIfAbsent(idRecurso, clave -> new ArrayList<>())
                    .add(Long.valueOf(fila[1]));
        }

        int recursosCreados = 0;
        int serviciosReapuntados = 0;
        for (Map.Entry<Long, List<Long>> entrada : proveedoresPorRecurso.entrySet()) {
            Long idRecurso = entrada.getKey();
            List<Long> proveedores = entrada.getValue();
            Long proveedorPrincipal = proveedores.get(0);
            if (existeRecurso(idRecurso)) {
                recursosCreados += jdbcTemplate.update(SQL_COMPLETAR,
                        nombrePrincipal(idRecurso), proveedorPrincipal, idRecurso);
            } else {
                jdbcTemplate.update(SQL_CREAR_CON_ID, idRecurso, nombrePrincipal(idRecurso), proveedorPrincipal);
                recursosCreados++;
            }
            for (int indice = 1; indice < proveedores.size(); indice++) {
                Long proveedorAdicional = proveedores.get(indice);
                Long nuevoId = jdbcTemplate.queryForObject(SQL_CREAR_CON_ID_DEVOLVIENDO_ID, Long.class,
                        siguienteId(), nombreReplica(idRecurso, proveedorAdicional), proveedorAdicional);
                jdbcTemplate.update(SQL_REAPUNTAR, nuevoId, idRecurso, proveedorAdicional);
                recursosCreados++;
                serviciosReapuntados++;
            }
        }
        return new int[]{recursosCreados, serviciosReapuntados};
    }

    private boolean existeRecurso(Long idRecurso) {
        return !jdbcTemplate.queryForList(SQL_EXISTE_RECURSO, Long.class, idRecurso).isEmpty();
    }

    private Long siguienteId() {
        return jdbcTemplate.queryForObject(SQL_MAX_ID, Long.class) + 1L;
    }

    private String nombrePrincipal(Long idRecurso) {
        return "Recurso " + idRecurso;
    }

    private String nombreReplica(Long idRecurso, Long idProveedor) {
        return nombrePrincipal(idRecurso) + " (proveedor " + idProveedor + ")";
    }

    private int marcarRecursosLegacy() {
        return jdbcTemplate.update(SQL_LEGACY);
    }

    private boolean crearIndiceUnico() {
        List<String> duplicados = jdbcTemplate.queryForList(SQL_DUPLICADOS, String.class);
        if (!duplicados.isEmpty()) {
            log.warn("Migracion recursos: {} nombres repetidos por proveedor, se omite el indice unico",
                    duplicados.size());
            return false;
        }
        if (!jdbcTemplate.queryForList(SQL_EXISTE_INDICE, String.class, TABLA, INDICE).isEmpty()) {
            return false;
        }
        jdbcTemplate.execute(SQL_CREAR_INDICE);
        return true;
    }

    public record Resultado(boolean identidadAgregada, int recursosCreados,
                            int serviciosReapuntados, int filasLegacy, boolean indiceCreado) {
    }
}
