package org.phuchoang.ecp.cart.api;

import java.util.UUID;

/** Transport-independent add-line intent. */
public record CartLineWrite(UUID variantId, int quantity) {
    public CartLineWrite {
        if (variantId == null || quantity <= 0) throw new IllegalArgumentException("A variant and positive quantity are required.");
    }
}
