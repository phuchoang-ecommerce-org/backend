package org.phuchoang.ecp.cart.internal.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Configured BR-CRT-01 inactivity windows, applied when a cart is created or touched. */
@ConfigurationProperties("ecp.cart.expiry")
public record CartLifetimeProperties(@DefaultValue("P7D") Duration guest,
                                     @DefaultValue("P30D") Duration customer,
                                     @DefaultValue("PT1M") Duration sweepDelay) {
    public CartLifetimeProperties {
        if (guest.isNegative() || guest.isZero() || customer.isNegative() || customer.isZero()
                || sweepDelay.isNegative() || sweepDelay.isZero()) {
            throw new IllegalArgumentException("Cart expiry durations must be positive.");
        }
    }
}
