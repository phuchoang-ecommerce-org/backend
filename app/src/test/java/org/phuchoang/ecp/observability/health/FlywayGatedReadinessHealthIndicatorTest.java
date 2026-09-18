package org.phuchoang.ecp.observability.health;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;

import static org.assertj.core.api.Assertions.assertThat;

/** L1 — `EN-OBS-1`: readiness reports DOWN until Flyway's migration strategy callback completes. */
class FlywayGatedReadinessHealthIndicatorTest {

    @Test
    void readinessIsDownBeforeMigrationRuns() {
        FlywayGatedReadinessHealthIndicator indicator = new FlywayGatedReadinessHealthIndicator();

        assertThat(indicator.health().getStatus()).isEqualTo(org.springframework.boot.health.contributor.Status.DOWN);
    }

    @Test
    void readinessIsUpOnlyAfterMigrateReturns() {
        FlywayGatedReadinessHealthIndicator indicator = new FlywayGatedReadinessHealthIndicator();
        Flyway flyway = Mockito.mock(Flyway.class);

        indicator.migrate(flyway);

        Mockito.verify(flyway).migrate();
        Health health = indicator.health();
        assertThat(health.getStatus()).isEqualTo(org.springframework.boot.health.contributor.Status.UP);
    }
}
