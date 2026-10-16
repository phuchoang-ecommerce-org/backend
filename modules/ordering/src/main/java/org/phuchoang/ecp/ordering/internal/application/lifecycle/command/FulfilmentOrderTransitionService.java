package org.phuchoang.ecp.ordering.internal.application.lifecycle.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Trusted system collaboration: Shipping enters through this use case, never Order persistence. */
@Service
@RequiredArgsConstructor
public class FulfilmentOrderTransitionService {
    private final OrderLifecycleDomainService lifecycle;
    @Transactional(readOnly = true) public Order require(UUID orderId) { return lifecycle.require(orderId); }
    @Transactional public Order advance(UUID orderId, OrderStatus target) { return lifecycle.persist(lifecycle.prepareTransition(orderId, target)); }
}
