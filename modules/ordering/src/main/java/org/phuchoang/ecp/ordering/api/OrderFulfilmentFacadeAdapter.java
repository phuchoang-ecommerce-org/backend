package org.phuchoang.ecp.ordering.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.application.lifecycle.command.FulfilmentOrderTransitionService;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderFulfilmentFacadeAdapter implements OrderFulfilmentFacade {
  private final FulfilmentOrderTransitionService transitions;

  @Override
  public OrderFulfilmentView getOrderForFulfilment(UUID orderId) {
    return view(transitions.require(orderId));
  }

  @Override
  public OrderFulfilmentView markDispatched(UUID orderId, UUID correlationId) {
    return view(transitions.advance(orderId, OrderStatus.SHIPPING));
  }

  @Override
  public OrderFulfilmentView markDelivered(UUID orderId, UUID correlationId) {
    return view(transitions.advance(orderId, OrderStatus.DELIVERED));
  }

  private static OrderFulfilmentView view(Order order) {
    return new OrderFulfilmentView(order.id(), order.customerId(), order.status().name());
  }
}
