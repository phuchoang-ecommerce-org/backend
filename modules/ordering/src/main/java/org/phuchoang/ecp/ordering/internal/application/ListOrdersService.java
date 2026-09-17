package org.phuchoang.ecp.ordering.internal.application;

import org.phuchoang.ecp.identity.api.authorization.AuthorizationService;
import org.phuchoang.ecp.identity.api.authorization.CallerContext;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.stereotype.Service;

/**
 * `UC-CUS-10` / `UC-ORD-06` / `UC-ADM-04` — View Purchase History (`US-CUS-10`): {@code listOrders}.
 *
 * <p>{@code ordering} has no order-placement code yet (roadmap: Sprint 18), so this always returns
 * an empty page — the sprint-05 backlog's Integration Risk note names this explicitly: the
 * envelope and cursor shape are the deliverable, not that rows come back. Authorisation still goes
 * through the real {@code identity.api.AuthorizationService}, the same cross-module Open Host
 * Service calls centralize their authorization decisions (`BR-AUD-02`) —
 * the decision must be identical regardless of which module makes it.
 */
@Service
public class ListOrdersService {

    static final String LIST_ORDERS = "listOrders";

    private final AuthorizationService authorizationService;

    public ListOrdersService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public OrderPageResult listOrders(CallerContext caller, ListOrdersQuery query) {
        authorizationService.assertAuthorized(caller, LIST_ORDERS);

        // customerId is Support/Administrator-only; a Customer supplying it is 403, per the contract.
        if (query.customerId() != null && caller.roleNames().contains("CUSTOMER")) {
            throw new DomainException(GenErrorCode.FORBIDDEN,
                "customerId may only be supplied by Support or Administrator.");
        }

        return OrderPageResult.EMPTY;
    }
}
