package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persistence child of {@link CartEntity}; there is intentionally no price column. */
@Entity
@Table(name = "cart_cart_line")
class CartLineEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cart_id", nullable = false) private CartEntity cart;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(nullable = false) private String sku;
    @Column(nullable = false) private int quantity;
    @Column(name = "added_at", nullable = false) private Instant addedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected CartLineEntity() { }
    CartLineEntity(UUID id, UUID variantId, String sku, int quantity, Instant addedAt, Instant now) {
        this.id = id; this.variantId = variantId; this.sku = sku; this.quantity = quantity; this.addedAt = addedAt;
        this.createdAt = now; this.updatedAt = now;
    }
    UUID id() { return id; } UUID variantId() { return variantId; } String sku() { return sku; }
    int quantity() { return quantity; } Instant addedAt() { return addedAt; }
    void attachTo(CartEntity parent) { cart = parent; }
    void synchronize(CartLineEntity replacement) { sku = replacement.sku; quantity = replacement.quantity; updatedAt = replacement.updatedAt; }
}
