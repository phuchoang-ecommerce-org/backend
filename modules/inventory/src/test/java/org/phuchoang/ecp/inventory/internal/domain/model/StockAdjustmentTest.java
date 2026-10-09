package org.phuchoang.ecp.inventory.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockAdjustmentTest {

    @Test
    void requiresTheAccountabilityInformationForAnAuthoritativeMovement() {
        assertThatThrownBy(() -> StockAdjustment.proposed(1, "COUNT", "", UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> StockAdjustment.proposed(1, "COUNT", "Cycle count", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transitionsFromProposedToRecordedOnlyOnce() {
        StockAdjustment proposed = StockAdjustment.proposed(1, "COUNT", "Cycle count", UUID.randomUUID());
        StockAdjustment recorded = proposed.recordFor(UUID.randomUUID(), UUID.randomUUID(),
            Instant.parse("2030-01-01T00:00:00Z"));

        assertThat(recorded.recorded()).isTrue();
        assertThatThrownBy(() -> recorded.recordFor(UUID.randomUUID(), UUID.randomUUID(), Instant.now()))
            .isInstanceOf(IllegalStateException.class);
    }
}
