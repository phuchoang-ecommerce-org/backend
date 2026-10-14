package org.phuchoang.ecp.promotion.internal.application.status.command;

import java.util.UUID;

/** Operational boundary for immediate lifecycle and compliance escalation. */
public interface PromotionLifecycleFailureReporter {
    void reportLifecycleFailure(UUID promotionId, String action, RuntimeException failure);
    void reportAuditGap(UUID promotionId, String action, RuntimeException failure);
}
