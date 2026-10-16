package org.phuchoang.ecp.payment.internal.application.callback.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ProviderNotification(String providerReference, String outcome, BigDecimal amount, String currency,
    String providerEventId, Instant occurredAt, Map<String, Object> providerPayload) {
}
