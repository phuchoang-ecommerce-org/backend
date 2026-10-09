package org.phuchoang.ecp.cart.api;

import org.phuchoang.ecp.cart.internal.application.cart.CartApplicationService;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Thin public adapter that keeps the application service module-private. */
@Component
class CartFacadeAdapter implements CartFacade {
    private final CartApplicationService carts;
    CartFacadeAdapter(CartApplicationService carts) { this.carts = carts; }
    @Override public CartView getCurrentCart(IdentityActor caller, String guestToken) { return carts.current(caller, guestToken); }
    @Override public CartView getCart(IdentityActor caller, String guestToken, UUID cartId) { return carts.get(caller, guestToken, cartId); }
    @Override public CartView addCartLine(IdentityActor caller, String guestToken, UUID cartId, CartLineWrite write) { return carts.add(caller, guestToken, cartId, write); }
    @Override public CartView updateCartLineQuantity(IdentityActor caller, String guestToken, UUID cartId, UUID lineId, int quantity) { return carts.changeQuantity(caller, guestToken, cartId, lineId, quantity); }
    @Override public void removeCartLine(IdentityActor caller, String guestToken, UUID cartId, UUID lineId) { carts.remove(caller, guestToken, cartId, lineId); }
}
