package org.phuchoang.ecp.cart.api;

import java.util.List;

/** Result of an attempted guest-cart claim after authentication has succeeded. */
public record CartMergeResult(boolean merged, List<CartMergeNotice> notices) {
    public CartMergeResult {
        notices = List.copyOf(notices);
    }

    public static CartMergeResult notFound() {
        return new CartMergeResult(false, List.of());
    }
}
