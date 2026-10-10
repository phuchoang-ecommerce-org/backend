package org.phuchoang.ecp.cart.internal.application.cart.expiry;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Default until Ordering supplies the checkout-state projection through Cart's port. */
@Component
class NoActiveCheckoutCartExpiryDeferral implements CartExpiryDeferral {
    @Override public boolean hasActiveCheckoutOrDraftOrder(UUID cartId) { return false; }
}
