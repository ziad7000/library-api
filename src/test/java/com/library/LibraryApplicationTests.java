package com.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryApplicationTests extends BaseIntegrationTest {

    @Test
    @DisplayName("Application context loads successfully with Testcontainers PostgreSQL")
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
    }
}
