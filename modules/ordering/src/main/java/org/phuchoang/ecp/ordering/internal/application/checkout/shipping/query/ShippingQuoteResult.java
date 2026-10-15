package org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShippingQuoteResult(List<Option> options, int shipmentCount) {
    public ShippingQuoteResult { options = List.copyOf(options); }
    public record Option(String id, String carrier, String serviceName, BigDecimal feeAmount, String currency,
            Instant estimatedDeliveryFrom, Instant estimatedDeliveryTo) { }
}
