package org.phuchoang.ecp.inventory.internal.domain.model;

/** Domain signal retaining the availability observed by the aggregate. */
public final class InsufficientAvailableStockException extends RuntimeException {

    private final int availableQuantity;

    public InsufficientAvailableStockException(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int availableQuantity() {
        return availableQuantity;
    }
}
