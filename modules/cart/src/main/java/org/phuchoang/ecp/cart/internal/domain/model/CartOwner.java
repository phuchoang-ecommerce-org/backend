package org.phuchoang.ecp.cart.internal.domain.model;

import java.util.Objects;
import java.util.UUID;

/** The cart owner used by command decisions; it deliberately has no HTTP or identity-framework dependency. */
public record CartOwner(UUID customerId, String guestToken) {
    public CartOwner {
        if (customerId == null && (guestToken == null || guestToken.isBlank())) {
            throw new IllegalArgumentException("A cart owner requires a customer or guest token.");
        }
        if (customerId != null && guestToken != null) {
            throw new IllegalArgumentException("A cart owner cannot be both customer and guest.");
        }
    }

    public static CartOwner customer(UUID customerId) {
        return new CartOwner(Objects.requireNonNull(customerId, "customerId"), null);
    }

    public static CartOwner guest(String guestToken) {
        return new CartOwner(null, guestToken);
    }

    public boolean owns(Cart cart) {
        return customerId != null ? customerId.equals(cart.customerId())
            : cart.customerId() == null && guestToken.equals(cart.guestToken());
    }
}
