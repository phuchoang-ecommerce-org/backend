package org.phuchoang.ecp.ordering.internal.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** The checkout's price snapshot for one cart line; refreshed before summary and placement. */
public record CheckoutLine(UUID id, UUID variantId, String sku, String productName, String variantName, int quantity,
        Money unitPrice) {
    public CheckoutLine {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(variantId, "variantId");
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity <= 0) throw new IllegalArgumentException("Checkout line quantity must be positive.");
    }

    public Money lineTotal() {
        return new Money(unitPrice.amount().multiply(BigDecimal.valueOf(quantity)), unitPrice.currency());
    }
}
