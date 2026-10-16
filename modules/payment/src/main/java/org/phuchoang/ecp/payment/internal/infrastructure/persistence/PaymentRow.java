package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentStatus;

/** JDBC representation owned solely by the Payment persistence adapter. */
record PaymentRow(UUID id, UUID orderId, UUID customerId, PaymentMethod method, BigDecimal authorisedAmount,
    String currency, PaymentStatus status, Instant createdAt, long version) {
}
