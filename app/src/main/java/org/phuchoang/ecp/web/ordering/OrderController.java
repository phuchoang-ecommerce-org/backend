package org.phuchoang.ecp.web.ordering;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.OrderPageView;
import org.phuchoang.ecp.ordering.api.OrderView;
import org.phuchoang.ecp.ordering.api.PlaceOrderRequest;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.phuchoang.ecp.web.common.pagination.PageEnvelope;
import org.phuchoang.ecp.web.common.pagination.Pagination;
import org.phuchoang.ecp.web.common.request.QueryParams;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;
import java.net.URI;
import org.springframework.http.ResponseEntity;

/**
 * `listOrders` (`UC-CUS-10`) — see {@code ordering.application.ListOrdersService}'s Javadoc: this
 * exposes placement and immutable order reads at the contract boundary.
 */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class OrderController {

    private static final Set<String> LIST_QUERY_PARAMS = Set.of(
        "cursor", "size", "sort", "status", "placedAfter", "placedBefore", "orderNumber", "customerId");

    private final OrderFacade orderFacade;
    private final RequestContextResolver requestContext;

    @GetMapping("/api/v1/orders")
    PageEnvelope<OrderView> listOrders(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
            @RequestParam(required = false) UUID customerId) {
        QueryParams.rejectUnknown(request, LIST_QUERY_PARAMS);
        int pageSize = Pagination.clampSize(size);

        OrderPageView page = orderFacade.listOrders(requestContext.resolve(jwt).caller(), cursor, pageSize, customerId);

        return new PageEnvelope<>(page.items(), new Page(page.items().size(), page.nextCursor(), null));
    }

    @PostMapping("/api/v1/orders")
    ResponseEntity<OrderView> place(@AuthenticationPrincipal Jwt jwt, @RequestBody PlaceOrderRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var context = requestContext.resolve(jwt);
        OrderView placed = orderFacade.placeOrder(context.caller(), body, idempotencyKey, context.correlationId());
        return ResponseEntity.created(URI.create("/api/v1/orders/" + placed.id())).body(placed);
    }

    @GetMapping("/api/v1/orders/{orderId}")
    OrderView getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return orderFacade.getOrder(requestContext.resolve(jwt).caller(), orderId);
    }

    @PostMapping("/api/v1/orders/{orderId}/status-transitions")
    OrderView advanceStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId,
            @Valid @RequestBody StatusTransition request) {
        var context = requestContext.resolve(jwt);
        return orderFacade.advanceOrderStatus(context.caller(), orderId, request.status(), request.reason(),
            context.correlationId());
    }

    record StatusTransition(@NotBlank String status, String reason) { }
}
