package org.phuchoang.ecp.ordering.api;

import java.util.UUID;

/** Shipping's narrow, state-machine-backed collaboration boundary. */
public interface OrderFulfilmentFacade {
    OrderFulfilmentView getOrderForFulfilment(UUID orderId);
    OrderFulfilmentView markDispatched(UUID orderId, UUID correlationId);
    OrderFulfilmentView markDelivered(UUID orderId, UUID correlationId);
    /** Consumer-side application of Payment's durable outcome event. */
    OrderFulfilmentView recordPaymentSucceeded(UUID orderId, UUID correlationId);
    OrderFulfilmentView recordPaymentFailed(UUID orderId, UUID correlationId);
}
