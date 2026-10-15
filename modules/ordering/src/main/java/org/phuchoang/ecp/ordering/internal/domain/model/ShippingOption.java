package org.phuchoang.ecp.ordering.internal.domain.model;

import java.time.Instant;
import java.util.Objects;

/** A provisional Sprint-17 quote shape; Shipping becomes the computation owner in Sprint 20. */
public record ShippingOption(String id, String carrier, String serviceName, Money fee, Instant estimatedDeliveryFrom,
        Instant estimatedDeliveryTo) {
    public ShippingOption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(carrier, "carrier");
        Objects.requireNonNull(fee, "fee");
    }
}
