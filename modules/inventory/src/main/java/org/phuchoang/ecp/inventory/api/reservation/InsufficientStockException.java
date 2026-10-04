package org.phuchoang.ecp.inventory.api.reservation;

import org.phuchoang.ecp.inventory.api.InventoryErrorCode;

import java.util.List;

/** ECP-INV-4091: a normal admission-control outcome, including under optimistic-lock races. */
public final class InsufficientStockException extends RuntimeException {

    private final List<StockShortfall> shortfalls;

    public InsufficientStockException(List<StockShortfall> shortfalls) {
        super("Insufficient available stock.");
        this.shortfalls = List.copyOf(shortfalls);
    }

    public InventoryErrorCode errorCode() {
        return InventoryErrorCode.INSUFFICIENT_STOCK;
    }

    public List<StockShortfall> shortfalls() {
        return shortfalls;
    }
}
