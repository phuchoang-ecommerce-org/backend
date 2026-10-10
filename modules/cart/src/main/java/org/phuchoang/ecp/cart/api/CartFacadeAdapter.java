package org.phuchoang.ecp.cart.api;

import org.phuchoang.ecp.cart.internal.application.cart.CartApplicationService;
import org.phuchoang.ecp.cart.internal.application.cart.command.AddCartLineCommand;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Thin public adapter that keeps the application service module-private. */
@Component
class CartFacadeAdapter implements CartFacade {
    private final CartApplicationService carts;
    CartFacadeAdapter(CartApplicationService carts) { this.carts = carts; }
    @Override public CartView getCurrentCart(IdentityActor caller, String guestToken) {
        return adapt(() -> cartView(carts.current(caller, guestToken)));
    }
    @Override public CartView getCart(IdentityActor caller, String guestToken, UUID cartId) {
        return adapt(() -> cartView(carts.get(caller, guestToken, cartId)));
    }
    @Override public CartView addCartLine(IdentityActor caller, String guestToken, UUID cartId, CartLineWrite write) {
        return adapt(() -> cartView(carts.add(caller, guestToken, cartId, new AddCartLineCommand(write.variantId(), write.quantity()))));
    }
    @Override public CartView updateCartLineQuantity(IdentityActor caller, String guestToken, UUID cartId, UUID lineId, int quantity) {
        return adapt(() -> cartView(carts.changeQuantity(caller, guestToken, cartId, lineId, quantity)));
    }
    @Override public void removeCartLine(IdentityActor caller, String guestToken, UUID cartId, UUID lineId) {
        adapt(() -> { carts.remove(caller, guestToken, cartId, lineId); return null; });
    }
    @Override public CartMergeResult mergeGuestCart(UUID customerId, String guestToken) {
        return adapt(() -> mergeResult(carts.mergeGuestCart(customerId, guestToken)));
    }

    private static CartView cartView(org.phuchoang.ecp.cart.internal.application.cart.query.CartView cart) {
        List<CartLineView> lines = cart.lines().stream().map(line -> new CartLineView(line.id(), line.variantId(), line.sku(),
            line.productName(), line.variantName(), line.quantity(), moneyView(line.unitPrice()), moneyView(line.lineTotal()),
            line.stockShort(), line.availableQuantity(), line.purchasable(), line.unpurchasableReason(), line.addedAt())).toList();
        return new CartView(cart.id(), cart.customerId(), cart.status(), lines, moneyView(cart.subtotal()), cart.lastActivityAt(),
            cart.expiresAt(), cart.expired());
    }

    private static MoneyView moneyView(org.phuchoang.ecp.cart.internal.application.cart.query.MoneyView money) {
        return money == null ? null : new MoneyView(money.amount(), money.currency());
    }

    private static CartMergeResult mergeResult(org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeResult result) {
        return new CartMergeResult(result.merged(), result.notices().stream().map(notice -> new CartMergeNotice(notice.variantId(),
            notice.sku(), notice.productName(), CartMergeNotice.Reason.valueOf(notice.reason().name()), notice.quantity())).toList());
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
