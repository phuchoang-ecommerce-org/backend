package org.phuchoang.ecp.payment.internal.application.get.query;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;

/** Read model for the order-payment resource; it is not an aggregate. */
public record PaymentDetails(UUID id, UUID orderId, UUID customerId, String method, BigDecimal authorisedAmount,
        BigDecimal capturedAmount, BigDecimal refundedAmount, String currency, String status, Instant createdAt) { }
