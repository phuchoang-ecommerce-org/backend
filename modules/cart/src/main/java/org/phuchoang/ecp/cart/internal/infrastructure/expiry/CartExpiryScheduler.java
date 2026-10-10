package org.phuchoang.ecp.cart.internal.infrastructure.expiry;

import org.phuchoang.ecp.cart.internal.application.cart.expiry.CartExpiryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduler adapter only; transaction and cart state transition live inward. */
@Component
class CartExpiryScheduler {
    private final CartExpiryLease lease;
    private final CartExpiryService expiry;

    CartExpiryScheduler(CartExpiryLease lease, CartExpiryService expiry) {
        this.lease = lease;
        this.expiry = expiry;
    }

    @Scheduled(fixedDelayString = "${ecp.cart.expiry.sweep-delay:PT1M}")
    void sweep() { lease.executeIfAcquired(expiry::expireDueCarts); }
}
