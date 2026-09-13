package org.phuchoang.ecp.catalog.api;

import org.phuchoang.ecp.catalog.application.query.GetProductService;
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

    /** Service behind the temporary authorization-wiring demonstration. */
    private final GetProductService getProductService;

    /**
     * Creates the demonstration facade.
     *
     * @param getProductService service that makes the authorization decision
     */
    public GetProductFacade(GetProductService getProductService) {
        this.getProductService = getProductService;
    }

    /**
     * Authorizes the caller through the demo service and returns its hard-coded product view.
     *
     * @param caller principal context whose permission is checked
     * @param productId identifier echoed by the demonstration response
     * @return non-persistent demo view
     */
    public ProductView getProduct(CallerContext caller, UUID productId) {
        org.phuchoang.ecp.catalog.application.query.ProductView view = getProductService.getProduct(caller, productId);
        return new ProductView(view.id(), view.name());
    }
}
