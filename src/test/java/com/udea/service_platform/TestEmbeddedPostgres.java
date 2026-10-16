package com.udea.service_platform;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;

/**
 * PostgreSQL embebido compartido para las pruebas @SpringBootTest.
 * Arranca un solo proceso postgres por JVM de pruebas (sin Docker) y lo
 * apaga al salir de la JVM para no dejar procesos huérfanos.
 */
public final class TestEmbeddedPostgres {

    public static final EmbeddedPostgres POSTGRES = start();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(TestEmbeddedPostgres::closeQuietly));
    }

    private TestEmbeddedPostgres() {
    }

    private static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo iniciar el PostgreSQL embebido de pruebas", e);
        }
    }

    private static void closeQuietly() {
        try {
            POSTGRES.close();
        } catch (IOException ignored) {
            // el proceso hijo se termina igualmente al cerrar la JVM
        }
    }
}
