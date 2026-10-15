package org.phuchoang.ecp.ordering.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.api.error.DomainException;
import org.phuchoang.ecp.ordering.api.error.GenErrorCode;
import org.phuchoang.ecp.ordering.internal.application.ListOrdersQuery;
import org.phuchoang.ecp.ordering.internal.application.ListOrdersService;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.application.place.command.PlaceOrderCommand;
import org.phuchoang.ecp.ordering.internal.application.place.command.PlaceOrderService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderFacadeAdapter implements OrderFacade {
    private final ListOrdersService listOrdersService;
    private final PlaceOrderService placeOrders;

    @Override
    public OrderPageView listOrders(IdentityActor caller, String cursor, int size, UUID customerId) {
        try {
            listOrdersService.listOrders(caller, new ListOrdersQuery(cursor, size, customerId));
            return OrderPageView.EMPTY;
        } catch (ApplicationException exception) {
            throw new DomainException(GenErrorCode.valueOf(exception.errorCode().name()), exception.getMessage());
        }
    }

    @Override
    public OrderPlacementView placeOrder(IdentityActor caller, PlaceOrderRequest request, String idempotencyKey,
            UUID correlationId) {
        try {
            var result = placeOrders.place(new PlaceOrderCommand(caller, request.checkoutId(), request.confirmationToken(),
                idempotencyKey, correlationId));
            return new OrderPlacementView(result.orderId(), result.status(), result.duplicate());
        } catch (ApplicationException exception) {
            throw switch (exception.errorCode()) {
                case IDEMPOTENCY_KEY_MISSING -> new DomainException(OrderErrorCode.IDEMPOTENCY_KEY_MISSING, exception.getMessage());
                case IDEMPOTENCY_KEY_REUSED -> new DomainException(OrderErrorCode.IDEMPOTENCY_KEY_REUSED, exception.getMessage());
                case CONFIRMATION_REQUIRED -> new DomainException(OrderErrorCode.TRANSITION_NOT_PERMITTED, exception.getMessage());
                case FORBIDDEN -> new DomainException(GenErrorCode.FORBIDDEN, exception.getMessage());
            };
        }
    }
}
