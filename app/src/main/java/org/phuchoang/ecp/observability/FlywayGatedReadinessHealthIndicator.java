package org.phuchoang.ecp.observability;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * `EN-OBS-1` (Sprint 04): readiness must fail while Flyway migrations are running. Flyway's own
 * {@code FlywayHealthIndicator} reports migration success/failure state, not "currently
 * migrating" — since Flyway runs synchronously at context startup by default, that distinction is
 * usually moot, but this makes the guarantee explicit and testable rather than an accident of
 * bean-initialisation ordering: providing a {@link FlywayMigrationStrategy} bean lets this class
 * flip its own flag only once {@link Flyway#migrate()} has actually returned, and the readiness
 * health group (`management.endpoint.health.group.readiness.include` in {@code application.yml})
 * reports {@code DOWN} until then.
 */
@Component
public class FlywayGatedReadinessHealthIndicator implements HealthIndicator, FlywayMigrationStrategy {

    private final AtomicBoolean migrationsComplete = new AtomicBoolean(false);

    @Override
    public void migrate(Flyway flyway) {
        flyway.migrate();
        migrationsComplete.set(true);
    }

    @Override
    public Health health() {
        if (migrationsComplete.get()) {
            return Health.up().build();
        }
        return Health.down().withDetail("reason", "Flyway migrations have not completed").build();
    }
}
