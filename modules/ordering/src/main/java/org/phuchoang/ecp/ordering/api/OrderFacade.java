package org.phuchoang.ecp.ordering.api;

import org.phuchoang.ecp.identity.api.authorization.CallerContext;
import org.phuchoang.ecp.ordering.application.ListOrdersQuery;
import org.phuchoang.ecp.ordering.application.ListOrdersService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** The reachable surface of {@code ordering} (`@NamedInterface`) — {@code app}'s web layer calls only this. */
@Component
public final class OrderFacade {

    private final ListOrdersService listOrdersService;

    OrderFacade(ListOrdersService listOrdersService) {
        this.listOrdersService = listOrdersService;
    }

    public OrderPageView listOrders(CallerContext caller, String cursor, int size, UUID customerId) {
        listOrdersService.listOrders(caller, new ListOrdersQuery(cursor, size, customerId));
        return OrderPageView.EMPTY;
    }
}
