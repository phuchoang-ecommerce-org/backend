package org.phuchoang.ecp.cart.api;

import java.util.Objects;
import java.util.UUID;

/** An explicitly reported adjustment made while folding a guest cart into a customer cart. */
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
