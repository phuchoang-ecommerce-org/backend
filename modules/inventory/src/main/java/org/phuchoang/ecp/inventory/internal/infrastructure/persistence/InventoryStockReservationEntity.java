package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Persistence-only child of {@link InventoryStockItemEntity}. */
@Entity
@Table(name = "inventory_stock_reservation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
