package com.udea.service_platform.modules.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null);

    private CorsConfiguration corsConfigurationFor(String origins) {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(origins);
        return ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");
    }

    @Test
    void corsConfiguration_usesOnlyDeclaredOrigins() {
        CorsConfiguration configuration =
                corsConfigurationFor("http://localhost:3000, https://frontend.ejemplo.com");

        assertEquals(List.of("http://localhost:3000", "https://frontend.ejemplo.com"),
                configuration.getAllowedOrigins());
        assertNull(configuration.getAllowedOriginPatterns());
    }

    @Test
    void corsConfiguration_restrictsHeadersToTheNeededOnes() {
        CorsConfiguration configuration = corsConfigurationFor("http://localhost:3000");

        assertEquals(List.of("Authorization", "Content-Type", "Accept"), configuration.getAllowedHeaders());
        assertTrue(configuration.getAllowedMethods().contains("PATCH"));
    }

    @Test
    void parseAllowedOrigins_ignoresEmptyEntriesAndTrimsSpaces() {
        assertEquals(List.of("http://localhost:3000", "http://localhost:5173"),
                SecurityConfig.parseAllowedOrigins(" http://localhost:3000 ,, http://localhost:5173 ,"));
    }

    @Test
    void parseAllowedOrigins_withWildcard_failsToStart() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SecurityConfig.parseAllowedOrigins("http://localhost:3000,*"));

        assertTrue(ex.getMessage().contains("comodines"));
    }

    @Test
    void parseAllowedOrigins_withWildcardInsideAnOrigin_failsToStart() {
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.parseAllowedOrigins("https://*.ejemplo.com"));
    }

    @Test
    void parseAllowedOrigins_withNullOrBlank_failsToStart() {
        assertThrows(IllegalStateException.class, () -> SecurityConfig.parseAllowedOrigins(null));
        assertThrows(IllegalStateException.class, () -> SecurityConfig.parseAllowedOrigins("   "));
    }

    @Test
    void parseAllowedOrigins_withOnlySeparators_failsToStart() {
        assertThrows(IllegalStateException.class, () -> SecurityConfig.parseAllowedOrigins(" , , "));
    }
}
