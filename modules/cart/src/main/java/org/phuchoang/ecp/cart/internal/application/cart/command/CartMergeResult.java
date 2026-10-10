package org.phuchoang.ecp.cart.internal.application.cart.command;

import java.util.List;

/** Application result of an attempted guest-cart claim. */
public record CartMergeResult(boolean merged, List<CartMergeNotice> notices) {
    public CartMergeResult {
        notices = List.copyOf(notices);
    }
}
