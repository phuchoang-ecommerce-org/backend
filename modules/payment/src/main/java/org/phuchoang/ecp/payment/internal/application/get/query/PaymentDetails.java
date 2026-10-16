package org.phuchoang.ecp.payment.internal.application.get.query;

import java.math.BigDecimal;
import java.util.UUID;

/** Read model for the order-payment resource; it is not an aggregate. */
public record PaymentDetails(UUID id, UUID orderId, UUID customerId, String method, BigDecimal authorisedAmount,
        String currency, String status) { }
