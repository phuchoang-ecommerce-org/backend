package org.phuchoang.ecp.web.ordering;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.identity.api.authorization.CallerContext;
import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.OrderPageView;
import org.phuchoang.ecp.web.pagination.Page;
import org.phuchoang.ecp.web.pagination.PageEnvelope;
import org.phuchoang.ecp.web.pagination.Pagination;
import org.phuchoang.ecp.web.request.QueryParams;
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

    OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @GetMapping("/api/v1/orders")
    PageEnvelope<Object> listOrders(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) UUID customerId) {
        QueryParams.rejectUnknown(request, LIST_QUERY_PARAMS);
        int pageSize = Pagination.clampSize(size);
        OrderPageView page = orderFacade.listOrders(callerOf(jwt), cursor, pageSize, customerId);
        return new PageEnvelope<>(page.items(), new Page(page.items().size(), page.nextCursor(), null));
    }

    private static CallerContext callerOf(Jwt jwt) {
        if (jwt == null) {
            return CallerContext.GUEST;
        }
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles"));
        return new CallerContext(UUID.fromString(jwt.getSubject()), roles);
    }
}
