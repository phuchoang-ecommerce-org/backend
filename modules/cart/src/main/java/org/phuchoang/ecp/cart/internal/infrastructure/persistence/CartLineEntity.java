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

/**
 * Persistence child of {@link CartEntity}; there is intentionally no price
 * column.
 */
@Entity
@Table(name = "cart_cart_line")
class CartLineEntity {
  @Id
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cart_id", nullable = false)
  private CartEntity cart;
  @Column(name = "variant_id", nullable = false)
  private UUID variantId;
  @Column(nullable = false)
  private String sku;
  @Column(nullable = false)
  private int quantity;
  @Column(name = "added_at", nullable = false)
  private Instant addedAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CartLineEntity() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public CartEntity getCart() {
    return cart;
  }

  public void setCart(CartEntity cart) {
    this.cart = cart;
  }

  public UUID getVariantId() {
    return variantId;
  }

  public void setVariantId(UUID variantId) {
    this.variantId = variantId;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public Instant getAddedAt() {
    return addedAt;
  }

  public void setAddedAt(Instant addedAt) {
    this.addedAt = addedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  void initializeAuditTimestamps(Instant now) {
    createdAt = now;
    updatedAt = now;
  }

  void attachTo(CartEntity parent) {
    cart = parent;
  }

  void synchronize(CartLineEntity replacement) {
    sku = replacement.sku;
    quantity = replacement.quantity;
    updatedAt = replacement.updatedAt;
  }
}
