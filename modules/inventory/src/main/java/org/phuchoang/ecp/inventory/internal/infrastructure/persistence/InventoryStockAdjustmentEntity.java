package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Persistence representation of an immutable inventory movement. */
@Entity
@Table(name = "inventory_stock_adjustment")
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

  protected InventoryStockAdjustmentEntity() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getStockItemId() {
    return stockItemId;
  }

  public void setStockItemId(UUID stockItemId) {
    this.stockItemId = stockItemId;
  }

  public int getDelta() {
    return delta;
  }

  public void setDelta(int delta) {
    this.delta = delta;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public UUID getActorId() {
    return actorId;
  }

  public void setActorId(UUID actorId) {
    this.actorId = actorId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
