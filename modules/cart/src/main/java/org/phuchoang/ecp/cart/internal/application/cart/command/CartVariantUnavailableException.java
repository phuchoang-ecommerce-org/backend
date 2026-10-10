package org.phuchoang.ecp.cart.internal.application.cart.command;

/** Application failure when a selected variant is not purchasable. */
public final class CartVariantUnavailableException extends RuntimeException {
    public CartVariantUnavailableException() {
        super("The selected variant is no longer purchasable.");
    }
}
