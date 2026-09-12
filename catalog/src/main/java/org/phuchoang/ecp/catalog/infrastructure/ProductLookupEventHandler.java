package org.phuchoang.ecp.catalog.infrastructure;

import org.phuchoang.ecp.catalog.application.GetProductService;
import org.phuchoang.ecp.identity.api.CallerContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sprint 04 (`US-AUD-03`) `BR-AUD-02` entry-point-parity demo only: proves the same
 * {@code AuthorizationService} decision is reached from a message-consumer-shaped entry point as
 * from REST, calling the identical {@link GetProductService}. Not a real {@code @KafkaListener} —
 * no Kafka dependency is wired into this repo yet (per the Sprint 04 Review Notes' "no
 * scheduler/Kafka infra yet" caveat). This proves the call path, not a broker integration; the
 * entry-point-parity test invokes {@link #handle(UUID)} directly as if a consumer had.
 */
@Component
public class ProductLookupEventHandler {

    private final GetProductService getProductService;

    public ProductLookupEventHandler(GetProductService getProductService) {
        this.getProductService = getProductService;
    }

    public void handle(UUID productId) {
        // A consumed event carries no interactive caller either — same reasoning as the
        // scheduler-shaped stub.
        getProductService.getProduct(CallerContext.GUEST, productId);
    }
}
