package org.phuchoang.ecp.identity.internal.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.regex.Pattern;

/** Postal address owned by Identity's customer address book. */
@ValueObject
public record Address(String label, String recipientName, String line1, String line2, String city, String region,
        String postalCode, String countryCode, String phone) {

    private static final Pattern COUNTRY_CODE = Pattern.compile("^[A-Z]{2}$");

    public Address {
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
