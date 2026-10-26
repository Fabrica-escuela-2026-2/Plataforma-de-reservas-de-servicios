package com.udea.service_platform.modules.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Fuente unica de la zona horaria de negocio y del reloj de aplicacion.
 * Los modulos convierten entre la hora local (America/Bogota) y UTC/Instant
 * usando esta constante, en vez de invocar systemDefaultZone directamente.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA_BOGOTA);
    }
}
