package org.phuchoang.ecp.ordering.internal.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable delivery/billing snapshot owned by the Checkout aggregate. */
@ValueObject
public record CheckoutAddress(String label, String recipientName, String line1, String line2, String city, String region,
        String postalCode, String countryCode, String phone) {
    private static final Pattern COUNTRY_CODE = Pattern.compile("^[A-Z]{2}$");

    public CheckoutAddress {
        Objects.requireNonNull(recipientName, "recipientName is required");
        Objects.requireNonNull(line1, "line1 is required");
        Objects.requireNonNull(city, "city is required");
        Objects.requireNonNull(postalCode, "postalCode is required");
        Objects.requireNonNull(countryCode, "countryCode is required");
        if (!COUNTRY_CODE.matcher(countryCode).matches()) {
            throw new IllegalArgumentException("countryCode must be an ISO 3166-1 alpha-2 code");
        }
    }
}
