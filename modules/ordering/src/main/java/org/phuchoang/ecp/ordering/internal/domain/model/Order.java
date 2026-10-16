package org.phuchoang.ecp.ordering.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The placed-order aggregate. Checkout values are copied into it at placement and are never
 * re-derived from Cart, Catalog, Identity, Inventory, or Promotion (BR-ORD-06).
 */
@AggregateRoot
public record Order(@Identity UUID id, UUID customerId, String orderNumber, List<CheckoutLine> lines,
        CheckoutAddress shippingAddress, CheckoutAddress billingAddress, ShippingOption shippingOption,
        UUID promotionId, String promotionCode, Money subtotal, Money discount, Money total,
        List<OrderReservation> reservations, OrderStatus status, long version) {

    public Order {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(orderNumber, "orderNumber");
        lines = List.copyOf(lines);
        reservations = List.copyOf(reservations);
        Objects.requireNonNull(shippingAddress, "shippingAddress");
        Objects.requireNonNull(billingAddress, "billingAddress");
        Objects.requireNonNull(shippingOption, "shippingOption");
        Objects.requireNonNull(subtotal, "subtotal");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(status, "status");
        if (lines.isEmpty() || reservations.isEmpty()) throw new NotReadyForPlacement();
        if (!subtotal.currency().equals(discount.currency()) || !subtotal.currency().equals(total.currency())
                || discount.amount().compareTo(subtotal.amount()) > 0) {
            throw new IllegalArgumentException("Order money must have one currency and bounded discount.");
        }
        BigDecimal expected = subtotal.amount().subtract(discount.amount()).add(shippingOption.fee().amount());
        if (total.amount().compareTo(expected) != 0) throw new IllegalArgumentException("Order total is inconsistent.");
    }

    /** The only placement transition: a fully reviewed checkout enters Pending Payment. */
    public static Order place(Checkout checkout, List<OrderReservation> reservations, Money bindingDiscount) {
        Objects.requireNonNull(checkout, "checkout");
        if (!checkout.ready() || checkout.confirmationToken() == null || checkout.confirmationToken().isBlank()) {
            throw new NotReadyForPlacement();
        }
        Money discount = bindingDiscount == null ? Money.zero(checkout.subtotal().currency()) : bindingDiscount;
        Money total = new Money(checkout.subtotal().amount().subtract(discount.amount()).add(checkout.shippingFee().amount()),
            checkout.subtotal().currency());
        return new Order(checkout.id(), checkout.customerId(), checkout.orderNumber(), checkout.lines(),
            checkout.shippingAddress(), checkout.billingAddress(), checkout.shippingOption(), checkout.promotionId(),
            checkout.promotionCode(), checkout.subtotal(), discount, total, reservations, OrderStatus.PENDING_PAYMENT,
            checkout.version());
    }

    /** Persists the complete aggregate only after an invariant-bearing operation has produced it. */
    public Order save(OrderRepository repository) {
        return repository.save(this);
    }

    /** Persists the complete aggregate lifecycle transition through its aggregate repository. */
    public Order saveTransition(OrderRepository repository) {
        return repository.saveTransition(this);
    }

    /** Applies one legal lifecycle edge. Authorization is a use-case policy; edge validity is not. */
    public Order transitionTo(OrderStatus target) {
        Objects.requireNonNull(target, "target");
        if (!OrderStateMachine.permits(status, target)) {
            throw new IllegalStatusTransition(status, target, OrderStateMachine.availableFrom(status));
        }
        return new Order(id, customerId, orderNumber, lines, shippingAddress, billingAddress, shippingOption,
            promotionId, promotionCode, subtotal, discount, total, reservations, target, version);
    }

    public static final class NotReadyForPlacement extends RuntimeException {
        public NotReadyForPlacement() { super("Checkout must be complete and re-confirmed before placement."); }
    }

    public static final class IllegalStatusTransition extends RuntimeException {
        private final OrderStatus current;
        private final List<OrderStatus> available;

        public IllegalStatusTransition(OrderStatus current, OrderStatus target, List<OrderStatus> available) {
            super("Order is " + current + "; allowed transitions: " + available);
            this.current = current;
            this.available = List.copyOf(available);
        }

        public OrderStatus current() { return current; }
        public List<OrderStatus> available() { return available; }
    }
}
