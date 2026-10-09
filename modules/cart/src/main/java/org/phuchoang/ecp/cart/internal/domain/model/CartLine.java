package org.phuchoang.ecp.cart.internal.domain.model;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A customer's selected variant. Deliberately carries no price (BR-CRT-04). */
@Entity
public record CartLine(@Identity UUID id, UUID variantId, String sku, int quantity, Instant addedAt) {
    public CartLine {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(variantId, "variantId");
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("A cart line requires a SKU.");
        if (quantity <= 0) throw new IllegalArgumentException("Cart line quantity must be positive.");
        Objects.requireNonNull(addedAt, "addedAt");
    }

    CartLine withQuantity(int newQuantity) {
        return new CartLine(id, variantId, sku, newQuantity, addedAt);
    }
}
