package org.phuchoang.ecp.inventory.internal.application.command.reservation.reserve;

import java.util.List;

/** Raised when the indivisible reserve-stock use case cannot be satisfied. */
public final class InsufficientStockException extends RuntimeException {

    private final List<StockShortfall> shortfalls;

    public InsufficientStockException(List<StockShortfall> shortfalls) {
        super("Insufficient available stock.");
        this.shortfalls = List.copyOf(shortfalls);
    }

    public List<StockShortfall> shortfalls() {
        return shortfalls;
    }
}
