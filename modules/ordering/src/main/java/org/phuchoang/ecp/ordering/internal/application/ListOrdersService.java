package org.phuchoang.ecp.ordering.internal.application;

import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

/**
 * `UC-CUS-10` / `UC-ORD-06` / `UC-ADM-04` — View Purchase History (`US-CUS-10`): {@code listOrders}.
 *
 * <p>{@code ordering} has no order-placement code yet (roadmap: Sprint 18), so this always returns
 * an empty page — the sprint-05 backlog's Integration Risk note names this explicitly: the
 * envelope and cursor shape are the deliverable, not that rows come back. Authorisation still goes
 * through the real {@code identity.api.IdentityAuthorization}, the same cross-module Open Host
 * Service calls centralize their authorization decisions (`BR-AUD-02`) —
 * the decision must be identical regardless of which module makes it.
 */
@Service
public class ListOrdersService {

    static final String LIST_ORDERS = "listOrders";

    private final IdentityAuthorization authorizationService;
    private final OrderRepository orders;

    public ListOrdersService(IdentityAuthorization authorizationService, OrderRepository orders) {
        this.authorizationService = authorizationService;
        this.orders = orders;
    }

    public OrderPageResult listOrders(IdentityActor caller, ListOrdersQuery query) {
        authorizationService.assertAuthorized(caller, LIST_ORDERS);

        // customerId is Support/Administrator-only; a Customer supplying it is 403, per the contract.
        if (query.customerId() != null && caller.roles().contains("CUSTOMER")) {
            throw new ApplicationException(ApplicationErrorCode.FORBIDDEN,
                "customerId may only be supplied by Support or Administrator.");
        }

        // Cursor extension is deliberately represented in the contract but not
        // inferred from a mutable offset. This first read-model slice serves a
        // customer's own newest snapshot rows; staff search remains separate.
        if (!caller.roles().contains("CUSTOMER")) return OrderPageResult.EMPTY;
        var page = orders.findPlacedByCustomerId(caller.accountId(), query.cursor(), query.size());
        return new OrderPageResult(page.items(), page.nextCursor());
    }
}
