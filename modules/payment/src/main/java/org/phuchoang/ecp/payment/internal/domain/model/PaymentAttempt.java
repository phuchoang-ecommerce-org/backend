package org.phuchoang.ecp.payment.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One durable provider request. Pending is deliberately an unresolved, not failed, state. */
public record PaymentAttempt(UUID id, String idempotencyKey, int attemptNumber, Money amount, AttemptOutcome outcome,
        String providerReference, String failureReason, Instant requestedAt, Instant resolvedAt) {
    public PaymentAttempt {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(requestedAt, "requestedAt");
        if (attemptNumber < 1 || amount.amount().signum() <= 0) throw new IllegalArgumentException("Invalid payment attempt.");
        if ((outcome == AttemptOutcome.PENDING) != (resolvedAt == null)) throw new IllegalArgumentException("Pending outcome has no resolution.");
    }

    static PaymentAttempt pending(String key, int number, Money amount, Instant requestedAt) {
        UUID id = UUID.randomUUID();
        return new PaymentAttempt(id, key, number, amount, AttemptOutcome.PENDING, "ecp-" + id, null, requestedAt, null);
    }

    PaymentAttempt resolve(AttemptOutcome result, String reference, Instant resolvedAt) {
        return new PaymentAttempt(id, idempotencyKey, attemptNumber, amount, result, reference,
            result == AttemptOutcome.FAILED ? "Payment was declined." : null, requestedAt, resolvedAt);
    }

    boolean same(AttemptOutcome result, BigDecimal amount, String reference) {
        return outcome == result && this.amount.amount().compareTo(amount) == 0 && Objects.equals(providerReference, reference);
    }
}
