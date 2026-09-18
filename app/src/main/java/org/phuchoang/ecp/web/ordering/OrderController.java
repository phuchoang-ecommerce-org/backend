package org.phuchoang.ecp.web.ordering;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.OrderPageView;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.phuchoang.ecp.web.common.pagination.PageEnvelope;
import org.phuchoang.ecp.web.common.pagination.Pagination;
import org.phuchoang.ecp.web.common.request.QueryParams;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/**
 * `listOrders` (`UC-CUS-10`) — see {@code ordering.application.ListOrdersService}'s Javadoc: this
 * always returns an empty page until Sprint 18 (`ordering` has no order-placement code yet).
 */
@RestController
class OrderController {

    private static final Set<String> LIST_QUERY_PARAMS = Set.of(
        "cursor", "size", "sort", "status", "placedAfter", "placedBefore", "orderNumber", "customerId");

    private final OrderFacade orderFacade;
    private final RequestContextResolver requestContext;

    OrderController(OrderFacade orderFacade, RequestContextResolver requestContext) {
        this.orderFacade = orderFacade;
        this.requestContext = requestContext;
    }

    @GetMapping("/api/v1/orders")
    PageEnvelope<Object> listOrders(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) UUID customerId) {
        QueryParams.rejectUnknown(request, LIST_QUERY_PARAMS);
        int pageSize = Pagination.clampSize(size);

        OrderPageView page = orderFacade.listOrders(requestContext.resolve(jwt).caller(), cursor, pageSize, customerId);

        return new PageEnvelope<>(page.items(), new Page(page.items().size(), page.nextCursor(), null));
    }
}
