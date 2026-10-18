package com.udea.service_platform;

import com.udea.service_platform.modules.users.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.sql.init.schema-locations=classpath:test-schema.sql")
class ServicePlatformApplicationTests {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TestEmbeddedPostgres.POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void contextLoadsAndInitializesPostgresData() {
        assertThat(roleRepository.findAll())
                .extracting("nombre")
                .contains("Cliente", "Proveedor de Servicios", "Administrador");
    }

}