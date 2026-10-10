package org.phuchoang.ecp.cart.internal.application.cart;

import org.phuchoang.ecp.cart.internal.application.cart.command.AddCartLineCommand;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeNotice;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeResult;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartNotFoundException;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartQuantityExceededException;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartVariantUnavailableException;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartLineView;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartOwner;
import org.phuchoang.ecp.cart.internal.domain.policy.CartMergePolicy;
import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartReadQuery;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartView;
import org.phuchoang.ecp.cart.internal.application.cart.query.MoneyView;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Command and authoritative PostgreSQL-backed read orchestration for Sprint 13. */
@Service
public class CartApplicationService {
    private final CartCommandService commands;
    private final CartReadQuery reads;
    private final VariantGateway variants;
    private final Clock clock;

    public CartApplicationService(CartCommandService commands, CartReadQuery reads, VariantGateway variants, Clock clock) {
        this.commands = commands; this.reads = reads; this.variants = variants; this.clock = clock;
    }

    @Transactional
    public CartView current(IdentityActor caller, String guestToken) {
        Cart cart = commands.current(owner(caller, guestToken), now());
        return cart.isExpiredAt(now()) ? expiredView(readModel(cart)) : view(readModel(cart), false);
    }

    @Transactional(readOnly = true)
    public CartView get(IdentityActor caller, String guestToken, UUID cartId) {
        CartReadQuery.CartReadModel cart = reads.readById(cartId).filter(candidate -> owns(owner(caller, guestToken), candidate))
            .orElseThrow(CartNotFoundException::new);
        return isExpired(cart) ? expiredView(cart) : view(cart, false);
    }

    @Transactional
    public CartView add(IdentityActor caller, String guestToken, UUID cartId, AddCartLineCommand command) {
        VariantGateway.Variant variant = requirePurchasable(command.variantId());
        return view(readModel(command(() -> commands.add(owner(caller, guestToken), cartId,
            new CartCommandService.Variant(variant.id(), variant.sku(), variant.availableQuantity()), command.quantity(), now()))), false);
    }

    @Transactional
    public CartView changeQuantity(IdentityActor caller, String guestToken, UUID cartId, UUID lineId, int quantity) {
        Cart changed = command(() -> commands.changeQuantity(owner(caller, guestToken), cartId, lineId,
            quantity, this::commandVariant, now()));
        return view(readModel(changed), false);
    }

    @Transactional
    public void remove(IdentityActor caller, String guestToken, UUID cartId, UUID lineId) {
        command(() -> { commands.remove(owner(caller, guestToken), cartId, lineId, now()); return null; });
    }

    /** UC-CRT-05. A transaction preserves both carts if persistence or concurrent updating fails. */
    @Transactional
    public CartMergeResult mergeGuestCart(UUID customerId, String guestToken) {
        CartCommandService.MergeResult result = commands.mergeGuestCart(customerId, guestToken, variantId -> variants.find(variantId)
            .map(value -> new CartMergePolicy.VariantAvailability(
                value.purchasable(), value.availableQuantity(), value.productName()))
            .orElse(null), now());
        return new CartMergeResult(result.merged(), result.notices().stream()
            .map(notice -> new CartMergeNotice(notice.variantId(), notice.sku(), notice.productName(),
                CartMergeNotice.Reason.valueOf(notice.reason().name()), notice.quantity()))
            .toList());
    }

    private CartView view(CartReadQuery.CartReadModel cart, boolean expired) {
        List<CartLineView> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = "USD";
        for (CartReadQuery.CartLineReadModel line : cart.lines()) {
            VariantGateway.Variant variant = variants.find(line.variantId()).orElse(null);
            Integer available = variant == null ? null : variant.availableQuantity();
            boolean purchasable = variant != null && variant.purchasable() && (available == null || available > 0);
            boolean shortStock = available != null && available < line.quantity();
            MoneyView price = variant == null ? null : new MoneyView(variant.price(), variant.currency());
            MoneyView lineTotal = price == null ? null : new MoneyView(price.amount().multiply(BigDecimal.valueOf(line.quantity())), price.currency());
            if (lineTotal != null) { subtotal = subtotal.add(lineTotal.amount()); currency = lineTotal.currency(); }
            lines.add(new CartLineView(line.id(), line.variantId(), line.sku(), variant == null ? null : variant.productName(),
                variant == null ? null : variant.variantName(), line.quantity(), price, lineTotal, shortStock, available,
                purchasable, unavailableReason(variant, available), line.addedAt()));
        }
        return new CartView(cart.id(), cart.customerId(), cart.status(), lines, new MoneyView(subtotal, currency),
            cart.lastActivityAt(), cart.expiresAt(), expired);
    }

    private CartView expiredView(CartReadQuery.CartReadModel cart) {
        return new CartView(cart.id(), cart.customerId(), cart.status(), List.of(), new MoneyView(BigDecimal.ZERO, "USD"),
            cart.lastActivityAt(), cart.expiresAt(), true);
    }
    private static String unavailableReason(VariantGateway.Variant variant, Integer available) {
        if (variant == null || !variant.purchasable()) return "Variant is no longer purchasable.";
        return available != null && available == 0 ? "Variant is currently out of stock." : null;
    }

    private VariantGateway.Variant requirePurchasable(UUID variantId) {
        VariantGateway.Variant variant = variants.find(variantId).orElseThrow(CartVariantUnavailableException::new);
        if (!variant.purchasable()) throw new CartVariantUnavailableException();
        return variant;
    }

    private CartCommandService.Variant commandVariant(UUID variantId) {
        VariantGateway.Variant variant = requirePurchasable(variantId);
        return new CartCommandService.Variant(variant.id(), variant.sku(), variant.availableQuantity());
    }

    private static boolean owns(CartOwner owner, CartReadQuery.CartReadModel cart) {
        return owner.customerId() != null ? owner.customerId().equals(cart.customerId())
            : cart.customerId() == null && owner.guestToken().equals(cart.guestToken());
    }
    private boolean isExpired(CartReadQuery.CartReadModel cart) {
        return "EXPIRED".equals(cart.status()) || !cart.expiresAt().isAfter(now());
    }
    private <T> T command(java.util.concurrent.Callable<T> operation) {
        try { return operation.call(); }
        catch (CartCommandService.CartNotFound exception) { throw new CartNotFoundException(); }
        catch (CartCommandService.QuantityExceeded exception) { throw new CartQuantityExceededException(exception.availableQuantity()); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private static CartReadQuery.CartReadModel readModel(Cart cart) {
        return new CartReadQuery.CartReadModel(cart.id(), cart.customerId(), cart.guestToken(), cart.status().name(), cart.lastActivityAt(),
            cart.expiresAt(), cart.lines().stream().map(line -> new CartReadQuery.CartLineReadModel(line.id(), line.variantId(), line.sku(),
                line.quantity(), line.addedAt())).toList());
    }
    private static CartOwner owner(IdentityActor caller, String guestToken) {
        return isCustomer(caller) ? CartOwner.customer(caller.accountId()) : CartOwner.guest(requireGuestToken(guestToken));
    }
    private static boolean isCustomer(IdentityActor caller) { return caller != null && caller.accountId() != null; }
    private static String requireGuestToken(String token) {
        if (token == null || token.isBlank()) throw new CartNotFoundException();
        return token;
    }
    private Instant now() { return clock.instant(); }
}
