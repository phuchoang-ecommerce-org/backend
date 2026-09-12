package org.phuchoang.ecp.catalog.api;

import org.phuchoang.ecp.catalog.application.GetProductService;
import org.phuchoang.ecp.identity.api.CallerContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The reachable surface for {@code catalog}'s Sprint 04 (`US-AUD-03`) RBAC wiring demo — see
 * {@link GetProductService}. Not a real catalog feature; exists so {@code app}'s web layer (and,
 * in the entry-point-parity tests, a scheduler/Kafka-shaped stub) can reach the demo service
 * without touching {@code catalog.application} directly
 * (`noClassReachesIntoAnotherModulesApplicationPackage`).
 */
@Component
public final class GetProductFacade {

    private final GetProductService getProductService;

    public GetProductFacade(GetProductService getProductService) {
        this.getProductService = getProductService;
    }

    public ProductView getProduct(CallerContext caller, UUID productId) {
        org.phuchoang.ecp.catalog.application.ProductView view = getProductService.getProduct(caller, productId);
        return new ProductView(view.id(), view.name());
    }
}
