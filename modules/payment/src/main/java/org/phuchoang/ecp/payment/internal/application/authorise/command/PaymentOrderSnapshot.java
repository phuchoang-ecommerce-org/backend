package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Locally owned copy of OrderCreated fields; Payment never reaches into
 * Ordering.
 */
public record PaymentOrderSnapshot(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency,
    String status) {
}
