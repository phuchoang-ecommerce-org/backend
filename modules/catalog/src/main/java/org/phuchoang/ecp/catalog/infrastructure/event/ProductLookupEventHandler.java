package org.phuchoang.ecp.catalog.infrastructure.event;

import org.phuchoang.ecp.catalog.application.query.GetProductService;
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

    /** Shared demo service that centralizes the authorization decision. */
    private final GetProductService getProductService;

    /**
     * Creates the message-consumer-shaped demo entry point.
     *
     * @param getProductService service invoked as if a message had requested a product lookup
     */
    public ProductLookupEventHandler(GetProductService getProductService) {
        this.getProductService = getProductService;
    }

    /**
     * Simulates an event-driven lookup using the system's guest-like demo caller. This does not
     * consume a broker message and exists only for entry-point-parity tests.
     *
     * @param productId identifier carried by the simulated event
     */
    public void handle(UUID productId) {
        // A consumed event carries no interactive caller either — same reasoning as the
        // scheduler-shaped stub.
        getProductService.getProduct(CallerContext.GUEST, productId);
    }
}
