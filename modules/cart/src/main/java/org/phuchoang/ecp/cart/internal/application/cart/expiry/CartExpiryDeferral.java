package org.phuchoang.ecp.cart.internal.application.cart.expiry;

import java.util.UUID;

/** Checkout-state port; Cart must not depend on Ordering or Inventory. */
public interface CartExpiryDeferral {
    boolean hasActiveCheckoutOrDraftOrder(UUID cartId);
}
