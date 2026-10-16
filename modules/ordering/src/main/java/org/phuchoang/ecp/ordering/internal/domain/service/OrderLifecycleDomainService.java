package org.phuchoang.ecp.ordering.internal.domain.service;

import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;

import java.util.UUID;

/** Domain entry point for lifecycle mutations; the aggregate remains the edge authority. */
public final class OrderLifecycleDomainService {
    private final OrderRepository orders;

    public OrderLifecycleDomainService(OrderRepository orders) { this.orders = orders; }

    public Order transition(UUID orderId, OrderStatus target) {
        return persist(prepareTransition(orderId, target));
    }

    /** Loads the authoritative aggregate and invokes its invariant-bearing transition exactly once. */
    public Transition prepareTransition(UUID orderId, OrderStatus target) {
        Order before = require(orderId);
        return new Transition(before, before.transitionTo(target));
    }

    public Order require(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFound(orderId));
    }

    public Order persist(Transition transition) {
        return transition.after().saveTransition(orders);
    }

    public static final class OrderNotFound extends RuntimeException {
        public OrderNotFound(UUID orderId) { super("Order not found: " + orderId); }
    }

    /** Domain result used by an application command to coordinate required effects around one aggregate edge. */
    public record Transition(Order before, Order after) { }
}
