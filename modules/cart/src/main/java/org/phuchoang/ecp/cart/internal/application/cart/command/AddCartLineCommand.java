package org.phuchoang.ecp.cart.internal.application.cart.command;

import java.util.UUID;

/** Application command for adding a purchasable variant to a cart. */
public record AddCartLineCommand(UUID variantId, int quantity) {
    public AddCartLineCommand {
        if (variantId == null || quantity <= 0) {
            throw new IllegalArgumentException("A variant and positive quantity are required.");
        }
    }
}
