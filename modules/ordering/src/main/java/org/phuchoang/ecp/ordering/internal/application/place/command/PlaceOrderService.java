package org.phuchoang.ecp.ordering.internal.application.place.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.phuchoang.ecp.ordering.internal.domain.service.PlaceOrderDomainService;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** The sole local transaction spanning placement, redemption, reservation, Order persistence, and outbox write. */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class PlaceOrderService {
    private static final Duration RESERVATION_HOLD = Duration.ofHours(24);
    private final IdentityAuthorization authorization;
    private final CheckoutDomainService checkouts;
    private final PlaceOrderDomainService orders;
    private final StockReservationPort stock;
    private final PromotionRedemptionPort promotions;
    private final OrderIdempotencyStore idempotency;
    private final OrderOutbox outbox;
    private final CartFacade carts;
    private final Clock clock;

    @Transactional
    public PlaceOrderResult place(PlaceOrderCommand command) {
        requireIdempotencyKey(command.idempotencyKey());
        authorization.assertAuthorized(command.caller(), "placeOrder");
        String hash = requestHash(command);
        var prior = idempotency.find(command.idempotencyKey());
        if (prior.isPresent()) return existing(prior.get(), command, hash);

        Checkout checkout = confirmedCheckout(command);
        if (!claim(command, hash, checkout.id())) {
            return existing(idempotency.find(command.idempotencyKey()).orElseThrow(), command, hash);
        }
        Money discount = redeemPromotion(checkout, command);
        List<OrderReservation> reservations = reserveStock(checkout, command);
        var order = orders.place(checkout, reservations, discount);
        appendOrderCreated(order, command);
        clearCustomerCart(command);
        return new PlaceOrderResult(order.id(), order.status().name(), false);
    }

    private void clearCustomerCart(PlaceOrderCommand command) {
        var cart = carts.getCurrentCart(command.caller(), null);
        for (var line : cart.lines()) {
            carts.removeCartLine(command.caller(), null, cart.id(), line.id());
        }
    }

    private Checkout confirmedCheckout(PlaceOrderCommand command) {
        Checkout checkout = checkouts.requireForPlacement(command.checkoutId(), command.caller().accountId());
        if (command.confirmationToken() == null || !command.confirmationToken().equals(checkout.confirmationToken())) {
            throw new ApplicationException(ApplicationErrorCode.CONFIRMATION_REQUIRED,
                "Checkout summary must be re-confirmed before placement.");
        }
        return checkout;
    }

    private boolean claim(PlaceOrderCommand command, String hash, UUID orderId) {
        return idempotency.claim(command.idempotencyKey(), command.caller().accountId(), hash, orderId);
    }

    private Money redeemPromotion(Checkout checkout, PlaceOrderCommand command) {
        if (checkout.promotionId() == null) return Money.zero(checkout.subtotal().currency());
        PromotionRedemptionPort.Discount redeemed = promotions.redeem(checkout.promotionId(), checkout.id(),
            command.caller().accountId(), checkout.subtotal().amount());
        return new Money(redeemed.amount(), redeemed.currency());
    }

    private List<OrderReservation> reserveStock(Checkout checkout, PlaceOrderCommand command) {
        return stock.reserve(checkout.id(), checkout.lines().stream()
            // The current Inventory contract identifies its StockItem. Until allocation becomes explicit in Shipping,
            // the checkout's variant snapshot is the existing placement identifier.
            .map(line -> new StockReservationPort.Line(line.id(), line.variantId(), line.sku(), line.quantity())).toList(),
            clock.instant().plus(RESERVATION_HOLD)).stream()
            .map(value -> new OrderReservation(value.orderLineId(), value.stockItemId(), value.warehouseId(),
                value.reservationId(), value.quantity()))
            .toList();
    }

    private void appendOrderCreated(org.phuchoang.ecp.ordering.internal.domain.model.Order order,
            PlaceOrderCommand command) {
        outbox.appendOrderCreated(order.id(), command.correlationId(), command.caller(), clock.instant(),
            "{\"orderId\":\"" + order.id() + "\",\"customerId\":\"" + order.customerId()
                + "\",\"totalAmount\":" + order.total().amount() + ",\"currency\":\"" + order.total().currency()
                + "\",\"status\":\"" + order.status().name() + "\"}");
    }

    private static PlaceOrderResult existing(OrderIdempotencyStore.Entry entry, PlaceOrderCommand command, String hash) {
        if (!entry.customerId().equals(command.caller().accountId()) || !entry.requestHash().equals(hash)) {
            throw new ApplicationException(ApplicationErrorCode.IDEMPOTENCY_KEY_REUSED,
                "Idempotency-Key belongs to a different request.");
        }
        return new PlaceOrderResult(entry.orderId(), "PENDING_PAYMENT", true);
    }

    private static void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) throw new ApplicationException(ApplicationErrorCode.IDEMPOTENCY_KEY_MISSING,
            "Idempotency-Key is required.");
        if (key.length() > 128) throw new ApplicationException(ApplicationErrorCode.IDEMPOTENCY_KEY_MISSING,
            "Idempotency-Key must be at most 128 characters.");
    }

    private static String requestHash(PlaceOrderCommand command) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                (command.checkoutId() + "\n" + command.confirmationToken()).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

}
