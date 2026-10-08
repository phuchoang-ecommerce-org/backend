package org.phuchoang.ecp.inventory.internal.application.command.reservation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ReservationRetryPolicyTest {

    @Test
    void acceptsAPositiveRetryLimit() {
        assertThat(new ReservationRetryPolicy(3).maxAttempts()).isEqualTo(3);
    }

    @Test
    void rejectsANonPositiveRetryLimit() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ReservationRetryPolicy(0))
            .withMessage("Reservation retry attempts must be at least 1.");
    }
}
