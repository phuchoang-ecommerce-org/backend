package org.phuchoang.ecp.ordering.internal.domain.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The single authoritative graph for BR-ORD-01 lifecycle edges. */
public final class OrderStateMachine {
    private static final Map<OrderStatus, List<OrderStatus>> TRANSITIONS = transitions();

    private OrderStateMachine() { }

    public static List<OrderStatus> availableFrom(OrderStatus current) {
        return TRANSITIONS.get(current);
    }

    public static boolean permits(OrderStatus current, OrderStatus target) {
        return availableFrom(current).contains(target);
    }

    private static Map<OrderStatus, List<OrderStatus>> transitions() {
        Map<OrderStatus, List<OrderStatus>> edges = new EnumMap<>(OrderStatus.class);
        edges.put(OrderStatus.DRAFT, List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        edges.put(OrderStatus.PENDING_PAYMENT, List.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED));
        edges.put(OrderStatus.PAID, List.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        edges.put(OrderStatus.PAYMENT_FAILED, List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        edges.put(OrderStatus.PROCESSING, List.of(OrderStatus.PACKED, OrderStatus.CANCELLED));
        edges.put(OrderStatus.PACKED, List.of(OrderStatus.SHIPPING));
        edges.put(OrderStatus.SHIPPING, List.of(OrderStatus.DELIVERED));
        edges.put(OrderStatus.DELIVERED, List.of(OrderStatus.COMPLETED, OrderStatus.RETURNED));
        edges.put(OrderStatus.CANCELLED, List.of(OrderStatus.REFUNDED));
        edges.put(OrderStatus.RETURNED, List.of(OrderStatus.REFUNDED));
        edges.put(OrderStatus.COMPLETED, List.of());
        edges.put(OrderStatus.REFUNDED, List.of());
        return Map.copyOf(edges);
    }
}
