package org.phuchoang.ecp.promotion.internal.application.status.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionLifecycleDomainService;
import org.springframework.stereotype.Service;

import java.time.Clock;

/** Finds due lifecycle work; each transition commits independently so failed work retries next sweep. */
@Service
@RequiredArgsConstructor
public class PromotionLifecycleSweepService {
    private static final int BATCH_SIZE = 100;
    private final PromotionLifecycleDomainService promotions;
    private final SetPromotionStatusService commands;
    private final PromotionLifecycleFailureReporter failures;
    private final Clock clock;

    public void processDueLifecycle() {
        var now = clock.instant();
        promotions.dueScheduledStatusChanges(now, BATCH_SIZE).forEach(promotion -> apply(promotion, "scheduled-status",
            () -> commands.applyScheduled(promotion.id(), now)));
        promotions.dueActivations(now, BATCH_SIZE).forEach(promotion -> apply(promotion, "activate",
            () -> commands.activateWindow(promotion.id(), now)));
        promotions.dueExpirations(now, BATCH_SIZE).forEach(promotion -> apply(promotion, "expire",
            () -> commands.expireWindow(promotion.id(), now)));
    }

    private void apply(Promotion promotion, String action, Runnable transition) {
        try { transition.run(); }
        catch (RuntimeException failure) { failures.reportLifecycleFailure(promotion.id(), action, failure); }
    }
}
