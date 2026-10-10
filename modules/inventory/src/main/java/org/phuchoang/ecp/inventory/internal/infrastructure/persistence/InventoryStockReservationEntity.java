package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persistence-only child of {@link InventoryStockItemEntity}. */
@Entity
@Table(name = "inventory_stock_reservation")
class InventoryStockReservationEntity {

  @Id
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stock_item_id", nullable = false)
  private InventoryStockItemEntity stockItem;
  @Column(name = "order_id", nullable = false)
  private UUID orderId;
  @Column(name = "order_line_id", nullable = false)
  private UUID orderLineId;
  @Column(nullable = false)
  private int quantity;
  @Column(nullable = false)
  private String status;
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
  @Column(name = "resolved_at")
  private Instant resolvedAt;
  @Column(name = "orphaned_at")
  private Instant orphanedAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected InventoryStockReservationEntity() {
    // JPA
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public InventoryStockItemEntity getStockItem() {
    return stockItem;
  }

  public void setStockItem(InventoryStockItemEntity stockItem) {
    this.stockItem = stockItem;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public UUID getOrderLineId() {
    return orderLineId;
  }

  public void setOrderLineId(UUID orderLineId) {
    this.orderLineId = orderLineId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public Instant getOrphanedAt() {
    return orphanedAt;
  }

  public void setOrphanedAt(Instant orphanedAt) {
    this.orphanedAt = orphanedAt;
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

  void initializeAuditTimestamps(Instant createdAt, Instant updatedAt) {
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  void attachTo(InventoryStockItemEntity item) {
    stockItem = item;
  }

  void updateFrom(InventoryStockReservationEntity source) {
    status = source.status;
    resolvedAt = source.resolvedAt;
    orphanedAt = source.orphanedAt;
    updatedAt = source.updatedAt;
  }
}
