package org.phuchoang.ecp.identity.api.view;

import java.util.List;
import java.util.UUID;

/**
 * `components/schemas/identity.yaml#/Session`. This backend is always a JSON API and never
 * issues browser cookies.
 */
public record SessionResponse(String accessToken, String refreshToken, long expiresIn, boolean restricted,
        AccountView account, CartMergeOutcome cartMerge) {

    public SessionResponse(String accessToken, String refreshToken, long expiresIn, boolean restricted, AccountView account) {
        this(accessToken, refreshToken, expiresIn, restricted, account, null);
    }

    /** Contract result for a guest-cart merge that must not prevent session creation. */
    public record CartMergeOutcome(boolean merged, UUID cartId, List<CartMergeAdjustment> adjustments) {
        public CartMergeOutcome {
            adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
        }
    }

    /** A single guest-cart line adjustment made during a successful merge. */
    public record CartMergeAdjustment(UUID variantId, String outcome, Integer requestedQuantity,
            Integer appliedQuantity, String detail) {
    }
}
