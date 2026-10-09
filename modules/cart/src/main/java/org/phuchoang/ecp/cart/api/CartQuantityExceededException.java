package org.phuchoang.ecp.cart.api;

/** ECP-CRT-4090: the caller's requested quantity remains unchanged. */
public final class CartQuantityExceededException extends RuntimeException {
    private final int availableQuantity;
    public CartQuantityExceededException(int availableQuantity) {
        super("Requested quantity exceeds available stock; " + availableQuantity + " available.");
        this.availableQuantity = availableQuantity;
    }
    public CartErrorCode errorCode() { return CartErrorCode.QUANTITY_EXCEEDS_STOCK; }
    public int availableQuantity() { return availableQuantity; }
}
