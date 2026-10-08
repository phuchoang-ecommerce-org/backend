package org.phuchoang.ecp.inventory.internal.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InventoryReservationPropertiesTest {

    @Test
    void acceptsAPositiveRetryLimit() {
        assertThat(new InventoryReservationProperties(3).maxAttempts()).isEqualTo(3);
    }

    @Test
    void rejectsANonPositiveRetryLimitAtStartup() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new InventoryReservationProperties(0))
            .withMessage("ecp.inventory.reservation.max-attempts must be at least 1.");
    }
}
