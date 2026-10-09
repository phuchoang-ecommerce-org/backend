package org.phuchoang.ecp.inventory.api.management;

import org.phuchoang.ecp.inventory.api.InventoryErrorCode;

import java.util.List;
import java.util.UUID;

/** ECP-INV-4091 with the reservations that prevent a negative physical stock correction. */
public final class StockAdjustmentDeclinedException extends RuntimeException {
    private final int reservedQuantity;
    private final List<UUID> orderIds;

    public StockAdjustmentDeclinedException(int reservedQuantity, List<UUID> orderIds) {
        super("The adjustment would reduce stock below units reserved for orders.");
        this.reservedQuantity = reservedQuantity;
        this.orderIds = List.copyOf(orderIds);
    }

    public InventoryErrorCode errorCode() { return InventoryErrorCode.INSUFFICIENT_STOCK; }
    public int reservedQuantity() { return reservedQuantity; }
    public List<UUID> orderIds() { return orderIds; }
}
