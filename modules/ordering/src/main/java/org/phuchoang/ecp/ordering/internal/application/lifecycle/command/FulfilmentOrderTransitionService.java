package org.phuchoang.ecp.ordering.internal.application.lifecycle.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Trusted system collaboration: Shipping enters through this use case, never
 * Order persistence.
 */
@Service
@RequiredArgsConstructor
public class FulfilmentOrderTransitionService {
    private final OrderLifecycleDomainService lifecycle;

    @Transactional(readOnly = true)
    public State require(UUID orderId) {
        return State.from(lifecycle.require(orderId));
    }

    @Transactional
    public State markDispatched(UUID orderId) {
        return advance(orderId, OrderStatus.SHIPPING);
    }

    @Transactional
    public State markDelivered(UUID orderId) {
        return advance(orderId, OrderStatus.DELIVERED);
    }

    @Transactional
    public State recordPaymentSucceeded(UUID orderId) {
        return advance(orderId, OrderStatus.PAID);
    }

    @Transactional
    public State recordPaymentFailed(UUID orderId) {
        return advance(orderId, OrderStatus.PAYMENT_FAILED);
    }

    private State advance(UUID orderId, OrderStatus target) {
        return State.from(lifecycle.persist(lifecycle.prepareTransition(orderId, target)));
    }

    /**
     * Application result mapped to the published facade without exposing the domain
     * model.
     */
    public record State(UUID orderId, UUID customerId, String status) {
        private static State from(Order order) {
            return new State(order.id(), order.customerId(), order.status().name());
        }
    }
}
