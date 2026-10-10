package org.phuchoang.ecp.cart.internal.application.cart.command;

/** Application failure when an advisory stock check rejects the requested quantity. */
public final class CartQuantityExceededException extends RuntimeException {
    private final int availableQuantity;

    public CartQuantityExceededException(int availableQuantity) {
        super("Requested quantity exceeds available stock; " + availableQuantity + " available.");
        this.availableQuantity = availableQuantity;
    }

    public int availableQuantity() {
        return availableQuantity;
    }
}
