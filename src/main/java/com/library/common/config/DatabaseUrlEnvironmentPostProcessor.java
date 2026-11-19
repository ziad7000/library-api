package com.library.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalizes cloud provider database URLs (e.g. Render, Heroku) where URLs are provided as
 * {@code postgresql://user:password@host[:port]/database} or {@code postgres://...} into
 * standard Spring Boot JDBC properties ({@code spring.datasource.url}, {@code username}, and {@code password}).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "databaseUrlNormalizer";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = environment.getProperty("spring.datasource.url");
        }

        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }

        // If it's already a standard JDBC URL without embedded user:pass@, leave it untouched
        if (rawUrl.startsWith("jdbc:postgresql:") && !rawUrl.contains("@")) {
            return;
        }

        // Process postgres://, postgresql://, or jdbc:postgresql:// with embedded credentials
        if (!rawUrl.startsWith("postgres://") && !rawUrl.startsWith("postgresql://") && !rawUrl.startsWith("jdbc:postgresql://")) {
            return;
        }

        try {
            String uriString = rawUrl;
            if (uriString.startsWith("jdbc:")) {
                uriString = uriString.substring(5);
            }
            if (uriString.startsWith("postgres://")) {
                uriString = "postgresql://" + uriString.substring("postgres://".length());
            }

            URI uri = URI.create(uriString);
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String query = uri.getQuery();

            if (host != null && path != null && !path.isBlank()) {
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path);

                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                }

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl.toString());

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    props.put("spring.datasource.username", URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                    if (parts.length > 1) {
                        props.put("spring.datasource.password", URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            }
        } catch (Exception ignored) {
            // If parsing fails, fall back gracefully to default Spring configuration
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
