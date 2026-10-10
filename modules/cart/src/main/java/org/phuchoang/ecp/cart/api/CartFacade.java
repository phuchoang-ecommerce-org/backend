package org.phuchoang.ecp.cart.api;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.UUID;

/** Public Cart application boundary used by the HTTP composition root. */
public interface CartFacade {
    CartView getCurrentCart(IdentityActor caller, String guestToken);
    CartView getCart(IdentityActor caller, String guestToken, UUID cartId);
    CartView addCartLine(IdentityActor caller, String guestToken, UUID cartId, CartLineWrite write);
    CartView updateCartLineQuantity(IdentityActor caller, String guestToken, UUID cartId, UUID lineId, int quantity);
    void removeCartLine(IdentityActor caller, String guestToken, UUID cartId, UUID lineId);
    CartMergeResult mergeGuestCart(UUID customerId, String guestToken);
}
