package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;

/** JDBC representation for an attempt owned by a PaymentRow. */
record PaymentAttemptRow(UUID id, UUID paymentId, String idempotencyKey, int attemptNumber, BigDecimal amount,
        String currency, AttemptOutcome outcome, String providerReference, String failureReason, Instant requestedAt,
        Instant resolvedAt) { }
