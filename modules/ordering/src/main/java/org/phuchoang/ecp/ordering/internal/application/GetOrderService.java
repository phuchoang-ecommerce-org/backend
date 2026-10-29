package org.phuchoang.ecp.ordering.internal.application;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Reads an order without revealing another customer's order exists. */
@Service
@RequiredArgsConstructor
public class GetOrderService {
    private final IdentityAuthorization authorization;
    private final OrderRepository orders;

    public Order get(IdentityActor caller, UUID orderId) {
        authorization.assertAuthorized(caller, "getOrder");
        Order order = orders.findById(orderId).orElseThrow(this::notFound);
        if (caller.roles().contains("CUSTOMER") && !caller.accountId().equals(order.customerId())) throw notFound();
        return order;
    }

    private ApplicationException notFound() {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Order not found.");
    }
}
