package org.phuchoang.ecp.payment.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void appliesOneSuccessfulResultAndNeverOverwritesItWithACounterpart() {
        Payment payment = Payment.begin(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.CARD,
            new Money(new BigDecimal("24.50"), "USD"), Instant.parse("2026-10-16T10:00:00Z"));
        PaymentAttempt attempt = payment.startAttempt("request-1", Instant.parse("2026-10-16T10:00:01Z"));

        Payment resolved = payment.withAttempt(attempt).resolve(attempt.id(), AttemptOutcome.SUCCEEDED, new BigDecimal("24.50"),
            "provider-ref", Instant.parse("2026-10-16T10:00:02Z"));

        assertThat(resolved.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(resolved.resolve(attempt.id(), AttemptOutcome.SUCCEEDED, new BigDecimal("24.50"), "provider-ref",
            Instant.parse("2026-10-16T10:00:03Z"))).isSameAs(resolved);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resolved.resolve(attempt.id(), AttemptOutcome.FAILED,
            new BigDecimal("24.50"), "provider-ref", Instant.parse("2026-10-16T10:00:03Z")))
            .isInstanceOf(Payment.ContradictoryResult.class);
    }
}
