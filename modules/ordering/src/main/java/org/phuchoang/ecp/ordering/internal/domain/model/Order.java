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

    /** Persists this complete aggregate through its domain-owned repository port. */
    public Order save(OrderRepository repository) {
        return repository.save(this);
    }

    public static final class NotReadyForPlacement extends RuntimeException {
        public NotReadyForPlacement() { super("Checkout must be complete and re-confirmed before placement."); }
    }
}
