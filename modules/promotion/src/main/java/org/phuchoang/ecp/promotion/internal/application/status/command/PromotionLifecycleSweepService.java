package org.phuchoang.ecp.promotion.internal.application.status.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.promotion.internal.application.status.query.DuePromotionQuery;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

/**
 * Finds due lifecycle work; each transition commits independently so failed
 * work retries next sweep.
 */
@Service
@RequiredArgsConstructor
public class PromotionLifecycleSweepService {
    private static final int BATCH_SIZE = 100;
    private final DuePromotionQuery duePromotions;
    private final SetPromotionStatusService commands;
    private final PromotionLifecycleFailureReporter failures;
    private final Clock clock;

    public void processDueLifecycle() {
        var now = clock.instant();
        duePromotions.scheduledStatusChanges(now, BATCH_SIZE)
                .forEach(promotionId -> apply(promotionId, "scheduled-status",
                        () -> commands.applyScheduled(promotionId, now)));
        duePromotions.activations(now, BATCH_SIZE).forEach(promotionId -> apply(promotionId, "activate",
                () -> commands.activateWindow(promotionId, now)));
        duePromotions.expirations(now, BATCH_SIZE).forEach(promotionId -> apply(promotionId, "expire",
                () -> commands.expireWindow(promotionId, now)));
    }

    private void apply(UUID promotionId, String action, Runnable transition) {
        try {
            transition.run();
        } catch (RuntimeException failure) {
            failures.reportLifecycleFailure(promotionId, action, failure);
        }
    }
}
