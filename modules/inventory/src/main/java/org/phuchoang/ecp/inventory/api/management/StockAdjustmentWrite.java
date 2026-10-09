package org.phuchoang.ecp.inventory.api.management;

/** Requested stock delta and the mandatory accountable reason. */
public record StockAdjustmentWrite(int delta, String reasonCode, String reason) {
    public StockAdjustmentWrite {
        if (delta == 0 || reasonCode == null || reasonCode.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inventory adjustment requires a non-zero delta and a reason.");
        }
    }
}
