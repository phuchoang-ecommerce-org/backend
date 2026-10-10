package org.phuchoang.ecp.cart.internal.application.cart.command;

/** Application failure for an absent or non-owned cart. */
public final class CartNotFoundException extends RuntimeException {
    public CartNotFoundException() {
        super("Cart was not found.");
    }
}
