package org.phuchoang.ecp.cart.internal.application.cart.command;

import java.util.Objects;
import java.util.UUID;

/** Application result detail for a guest-cart merge adjustment. */
public record CartMergeNotice(UUID variantId, String sku, String productName, Reason reason, Integer quantity) {
    public CartMergeNotice {
        Objects.requireNonNull(variantId, "variantId");
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(reason, "reason");
    }

    public enum Reason {
        DROPPED_UNPUBLISHED, QUANTITY_REDUCED_TO_AVAILABLE, RETAINED_OUT_OF_STOCK
    }
}
