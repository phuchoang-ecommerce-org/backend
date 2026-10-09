package org.phuchoang.ecp.inventory.internal.application.management.command.adjust;

import java.util.List;
import java.util.UUID;

/** Application failure when a correction would consume units already reserved for orders. */
public final class StockAdjustmentDeclinedException extends RuntimeException {
    private final int reservedQuantity;
    private final List<UUID> orderIds;

    public StockAdjustmentDeclinedException(int reservedQuantity, List<UUID> orderIds) {
        super("The adjustment would reduce stock below units reserved for orders.");
        this.reservedQuantity = reservedQuantity;
        this.orderIds = List.copyOf(orderIds);
    }

    public int reservedQuantity() { return reservedQuantity; }
    public List<UUID> orderIds() { return orderIds; }
}
