package org.phuchoang.ecp.catalog.infrastructure;

import org.phuchoang.ecp.catalog.application.GetProductService;
import org.phuchoang.ecp.identity.api.CallerContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sprint 04 (`US-AUD-03`) `BR-AUD-02` entry-point-parity demo only: proves the same
 * {@code AuthorizationService} decision is reached from a scheduler-shaped entry point as from
 * REST, calling the identical {@link GetProductService}. Not wired to Spring's {@code @Scheduled}
 * trigger — no real scheduled job exists in this codebase yet (per the Sprint 04 Review Notes'
 * "no scheduler/Kafka infra yet" caveat) — the entry-point-parity test invokes {@link #run()}
 * directly to prove the call path, not the trigger.
 */
@Component
public class ProductLookupScheduledTask {

    private final GetProductService getProductService;

    public ProductLookupScheduledTask(GetProductService getProductService) {
        this.getProductService = getProductService;
    }

    public void run() {
        // A scheduled job has no interactive caller — it acts with the system's own authority.
        // Once a real scheduled job exists, it would use a role reflecting that, not GUEST.
        getProductService.getProduct(CallerContext.GUEST, UUID.randomUUID());
    }
}
