package org.phuchoang.ecp.ordering.api;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.api.error.DomainException;
import org.phuchoang.ecp.ordering.api.error.GenErrorCode;
import org.phuchoang.ecp.ordering.internal.application.ListOrdersQuery;
import org.phuchoang.ecp.ordering.internal.application.ListOrdersService;
import org.phuchoang.ecp.ordering.internal.application.GetOrderService;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.application.place.command.PlaceOrderCommand;
import org.phuchoang.ecp.ordering.internal.application.place.command.PlaceOrderService;
import org.phuchoang.ecp.ordering.internal.application.lifecycle.command.AdvanceOrderStatusCommand;
import org.phuchoang.ecp.ordering.internal.application.lifecycle.command.AdvanceOrderStatusService;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderFacadeAdapter implements OrderFacade {
    private final ListOrdersService listOrdersService;
    private final GetOrderService getOrderService;
    private final PlaceOrderService placeOrders;
    private final AdvanceOrderStatusService advanceOrderStatus;
    private final OrderApiMapper mapper;

    @Override
    public OrderPageView listOrders(IdentityActor caller, String cursor, int size, UUID customerId) {
        try {
            var page = listOrdersService.listOrders(caller, new ListOrdersQuery(cursor, size, customerId));
            return new OrderPageView(page.items().stream().map(mapper::orderView).toList(), page.nextCursor());
        } catch (ApplicationException exception) {
            throw new DomainException(GenErrorCode.valueOf(exception.errorCode().name()), exception.getMessage());
        }
    }

    @Override
    public OrderView getOrder(IdentityActor caller, UUID orderId) {
        try {
            return mapper.orderView(getOrderService.get(caller, orderId));
        } catch (ApplicationException exception) {
            throw new DomainException(GenErrorCode.valueOf(exception.errorCode().name()), exception.getMessage());
        }
    }

    @Override
    public OrderView placeOrder(IdentityActor caller, PlaceOrderRequest request, String idempotencyKey,
            UUID correlationId) {
        try {
            var result = placeOrders.place(new PlaceOrderCommand(caller, request.checkoutId(), request.confirmationToken(),
                idempotencyKey, correlationId));
            return getOrder(caller, result.orderId());
        } catch (ApplicationException exception) {
            throw switch (exception.errorCode()) {
                case IDEMPOTENCY_KEY_MISSING -> new DomainException(OrderErrorCode.IDEMPOTENCY_KEY_MISSING, exception.getMessage());
                case IDEMPOTENCY_KEY_REUSED -> new DomainException(OrderErrorCode.IDEMPOTENCY_KEY_REUSED, exception.getMessage());
                case CONFIRMATION_REQUIRED -> new DomainException(OrderErrorCode.TRANSITION_NOT_PERMITTED, exception.getMessage());
                case FORBIDDEN -> new DomainException(GenErrorCode.FORBIDDEN, exception.getMessage());
                case NOT_FOUND -> new DomainException(GenErrorCode.NOT_FOUND, exception.getMessage());
            };
        }
    }

    @Override
    public OrderView advanceOrderStatus(IdentityActor caller, UUID orderId, String status, String reason,
            UUID correlationId) {
        try {
            Order advanced = advanceOrderStatus.advance(new AdvanceOrderStatusCommand(caller, orderId,
                OrderStatus.valueOf(status), reason, correlationId));
            return mapper.orderView(advanced);
        } catch (Order.IllegalStatusTransition exception) {
            throw new DomainException(OrderErrorCode.TRANSITION_NOT_PERMITTED, exception.getMessage());
        } catch (org.phuchoang.ecp.ordering.internal.domain.service.OrderLifecycleDomainService.OrderNotFound exception) {
            throw new DomainException(GenErrorCode.NOT_FOUND, "Order not found.");
        }
    }
}
