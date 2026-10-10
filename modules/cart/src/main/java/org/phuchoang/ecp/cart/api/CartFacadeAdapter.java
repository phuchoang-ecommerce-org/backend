package org.phuchoang.ecp.cart.api;

import org.phuchoang.ecp.cart.internal.application.cart.CartApplicationService;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/** Thin public adapter that keeps the application service module-private. */
@Component
class CartFacadeAdapter implements CartFacade {
  private final CartApplicationService carts;
  private final CartApiMapper mapper;

  CartFacadeAdapter(CartApplicationService carts, CartApiMapper mapper) {
    this.carts = carts;
    this.mapper = mapper;
  }

  @Override
  public CartView getCurrentCart(IdentityActor caller, String guestToken) {
    return adapt(() -> mapper.cartView(carts.current(caller, guestToken)));
  }

  @Override
  public CartView getCart(IdentityActor caller, String guestToken, UUID cartId) {
    return adapt(() -> mapper.cartView(carts.get(caller, guestToken, cartId)));
  }

  @Override
  public CartView addCartLine(IdentityActor caller, String guestToken, UUID cartId, CartLineWrite write) {
    return adapt(() -> mapper.cartView(carts.add(caller, guestToken, cartId, mapper.addCartLineCommand(write))));
  }

  @Override
  public CartView updateCartLineQuantity(IdentityActor caller, String guestToken, UUID cartId, UUID lineId,
      int quantity) {
    return adapt(() -> mapper.cartView(carts.changeQuantity(caller, guestToken, cartId, lineId, quantity)));
  }

  @Override
  public void removeCartLine(IdentityActor caller, String guestToken, UUID cartId, UUID lineId) {
    adapt(() -> {
      carts.remove(caller, guestToken, cartId, lineId);
      return null;
    });
  }

  @Override
  public CartMergeResult mergeGuestCart(UUID customerId, String guestToken) {
    return adapt(() -> mapper.mergeResult(carts.mergeGuestCart(customerId, guestToken)));
  }

  private static <T> T adapt(Supplier<T> operation) {
    try {
      return operation.get();
    } catch (org.phuchoang.ecp.cart.internal.application.cart.command.CartNotFoundException exception) {
      throw new CartNotFoundException();
    } catch (org.phuchoang.ecp.cart.internal.application.cart.command.CartQuantityExceededException exception) {
      throw new CartQuantityExceededException(exception.availableQuantity());
    } catch (org.phuchoang.ecp.cart.internal.application.cart.command.CartVariantUnavailableException exception) {
      throw new CartVariantUnavailableException();
    }
  }
}
