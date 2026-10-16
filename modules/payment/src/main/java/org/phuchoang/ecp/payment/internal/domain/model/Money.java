package org.phuchoang.ecp.payment.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Payment-owned monetary value; payment never receives an instrument. */
public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0 || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Money must be non-negative with an ISO currency.");
        }
    }
}
