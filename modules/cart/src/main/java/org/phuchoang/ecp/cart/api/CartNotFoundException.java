package org.phuchoang.ecp.cart.api;

/** Deliberately uniform outcome for absent and non-owned carts. */
public final class CartNotFoundException extends RuntimeException {
    public CartNotFoundException() { super("Cart was not found."); }
}
