package org.phuchoang.ecp.ordering.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.application.lifecycle.command.FulfilmentOrderTransitionService;
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
    return view(transitions.markDispatched(orderId));
  }

  @Override
  public OrderFulfilmentView markDelivered(UUID orderId, UUID correlationId) {
    return view(transitions.markDelivered(orderId));
  }

  @Override
  public OrderFulfilmentView recordPaymentSucceeded(UUID orderId, UUID correlationId) {
    return view(transitions.recordPaymentSucceeded(orderId));
  }

  @Override
  public OrderFulfilmentView recordPaymentFailed(UUID orderId, UUID correlationId) {
    return view(transitions.recordPaymentFailed(orderId));
  }

  private static OrderFulfilmentView view(FulfilmentOrderTransitionService.State state) {
    return new OrderFulfilmentView(state.orderId(), state.customerId(), state.status());
  }
}
