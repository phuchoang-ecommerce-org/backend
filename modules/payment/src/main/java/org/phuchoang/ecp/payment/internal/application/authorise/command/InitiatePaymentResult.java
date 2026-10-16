package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.net.URI;
import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentResult(UUID paymentId, UUID orderId, UUID customerId, String method,
        BigDecimal authorisedAmount, String currency, String status, UUID attemptId, int attemptNumber,
        String outcome, String providerReference, URI redirectUrl, boolean accepted) {
}
