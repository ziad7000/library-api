package com.library.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("Normalizes Render connection string with missing port to standard JDBC URL and credentials")
    void normalizesRenderUrlWithoutPort() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "SPRING_DATASOURCE_URL", "postgresql://library_user:super_secret@dpg-c123456789-a/library_v8je"
        )));

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-c123456789-a:5432/library_v8je");
        assertThat(env.getProperty("spring.datasource.username"))
                .isEqualTo("library_user");
        assertThat(env.getProperty("spring.datasource.password"))
                .isEqualTo("super_secret");
    }

    @Test
    @DisplayName("Normalizes postgres:// scheme with explicit port and query parameters")
    void normalizesPostgresSchemeWithPortAndParams() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgres://admin_user:pass123@myhost:5433/production_db?sslmode=require"
        )));

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://myhost:5433/production_db?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username"))
                .isEqualTo("admin_user");
        assertThat(env.getProperty("spring.datasource.password"))
                .isEqualTo("pass123");
    }

    @Test
    @DisplayName("Leaves standard JDBC URL without user credentials untouched")
    void leavesStandardJdbcUrlUntouched() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/library"
        )));

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }

    @Test
    @DisplayName("Handles empty or null URLs gracefully without adding property sources")
    void handlesNullOrEmptyGracefully() {
        StandardEnvironment env = new StandardEnvironment();

        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
    }
}
