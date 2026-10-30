package org.phuchoang.ecp.ordering.api;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.UUID;

/** Public facade for order-list queries. Its adapter is the API-to-application boundary. */
public interface OrderFacade {
    OrderPageView listOrders(IdentityActor caller, String cursor, int size, UUID customerId);
    OrderView getOrder(IdentityActor caller, UUID orderId);
    OrderView placeOrder(IdentityActor caller, PlaceOrderRequest request, String idempotencyKey, UUID correlationId);
    OrderView advanceOrderStatus(IdentityActor caller, UUID orderId, String status, String reason, UUID correlationId);
}
