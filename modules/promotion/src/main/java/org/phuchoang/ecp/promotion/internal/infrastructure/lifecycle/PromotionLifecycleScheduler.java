package org.phuchoang.ecp.promotion.internal.infrastructure.lifecycle;

import org.phuchoang.ecp.promotion.internal.application.status.command.PromotionLifecycleSweepService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduler adapter only; lifecycle behavior and transactions remain in Application and Domain. */
@Component
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class PromotionLifecycleScheduler {
    private final PromotionLifecycleLease lease;
    private final PromotionLifecycleSweepService lifecycle;

    @Scheduled(fixedDelayString = "${ecp.promotion.lifecycle.sweep-delay:PT1M}")
    void sweep() { lease.executeIfAcquired(lifecycle::processDueLifecycle); }
}
