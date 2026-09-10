package org.phuchoang.ecp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * L4-L6 container wiring for the {@code integrationTest} source set (EN-DATA-1).
 * PostgreSQL only, matching {@code postgres:16} in {@code compose.yaml} — the production image and
 * version, per {@code ADR-0009} and {@code Deployment Diagram.md}. Elasticsearch, Kafka, MongoDB, and
 * Redis containers are not wired here; none of the four are yet used by any test in this repository.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withReuse(true);
    }
}
