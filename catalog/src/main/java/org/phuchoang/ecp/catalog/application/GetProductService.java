package org.phuchoang.ecp.catalog.application;

import org.phuchoang.ecp.identity.api.AuthorizationService;
import org.phuchoang.ecp.identity.api.CallerContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sprint 04 (`US-AUD-03`) wiring demonstration only — {@code catalog} has no product read model or
 * OpenAPI-backed endpoint yet (Flyway's `catalog_*` tables exist ahead of code, per the module
 * roadmap). This service exists solely to prove {@code identity.api.AuthorizationService} is
 * reachable from another module's {@code application} layer, and that the same authorisation
 * decision is reached identically through different entry-point shapes (`BR-AUD-02`) — see the
 * REST ({@code ProductController}), scheduler, and Kafka-shaped call sites that all delegate here.
 * The returned {@link ProductView} is a hardcoded stub, not real catalog data.
 */
@Service
public class GetProductService {

    static final String GET_PRODUCT = "getProduct";

    private final AuthorizationService authorizationService;

    public GetProductService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public ProductView getProduct(CallerContext caller, UUID productId) {
        authorizationService.assertAuthorized(caller, GET_PRODUCT);
        return new ProductView(productId, "Sprint 04 RBAC wiring demo — not real catalog data");
    }
}
