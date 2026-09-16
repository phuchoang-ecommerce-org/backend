package org.phuchoang.ecp.sharedkernel.api.address;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A postal address (`Domain Model.md` §5.3, `components/schemas/common.yaml#/Address`). The same
 * shape serves a customer's address-book entry and the immutable snapshot frozen onto an order at
 * placement (`BR-ORD-06`) — this type carries no Spring/JPA/Jackson dependency so both `identity`
 * and (later) `ordering` can hold it as a plain value object (`Module Dependency Diagram.md` §7).
 *
 * <p>{@code label}, {@code line2}, {@code region}, and {@code phone} are optional; every other
 * field is required, matching the OpenAPI schema's {@code required} list exactly.
 */
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
