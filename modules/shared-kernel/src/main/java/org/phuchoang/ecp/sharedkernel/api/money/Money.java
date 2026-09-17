package org.phuchoang.ecp.sharedkernel.api.money;

import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Exact monetary value shared by domain models. Wire DTOs deliberately map this value to their
 * own representation so framework concerns never enter the shared kernel.
 */
@ValueObject
public record Money(BigDecimal amount, String currency) {

    private static final Pattern ISO_4217 = Pattern.compile("^[A-Z]{3}$");

    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (amount.scale() > 4 || amount.precision() > 19) {
            throw new IllegalArgumentException("amount must fit NUMERIC(19,4)");
        }
        if (!ISO_4217.matcher(currency).matches()) {
            throw new IllegalArgumentException("currency must be an ISO 4217 code");
        }
    }
}
