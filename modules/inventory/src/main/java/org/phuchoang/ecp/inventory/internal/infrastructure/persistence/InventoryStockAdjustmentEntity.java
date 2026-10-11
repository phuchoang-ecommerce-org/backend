package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Persistence representation of an immutable inventory movement. */
@Entity
@Table(name = "inventory_stock_adjustment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class InventoryStockAdjustmentEntity {
  @Id
  private UUID id;
  @Column(name = "stock_item_id", nullable = false)
  private UUID stockItemId;
  @Column(nullable = false)
  private int delta;
  @Column(name = "reason_code", nullable = false)
  private String reasonCode;
  @Column(nullable = false)
  private String reason;
  @Column(name = "actor_id", nullable = false)
  private UUID actorId;
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

}
