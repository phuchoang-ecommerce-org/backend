package org.phuchoang.ecp.cart.api;

/** A removed or unpublished variant cannot be newly added to a cart. */
public final class CartVariantUnavailableException extends RuntimeException {
    public CartVariantUnavailableException() { super("The selected variant is no longer purchasable."); }
}
