package org.phuchoang.ecp.promotion.internal.infrastructure.lifecycle;

import org.phuchoang.ecp.promotion.internal.application.status.command.PromotionLifecycleFailureReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Emits immediate, structured operational/compliance escalations without hiding failed lifecycle work. */
@Component
class LoggingPromotionLifecycleFailureReporter implements PromotionLifecycleFailureReporter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingPromotionLifecycleFailureReporter.class);

    @Override public void reportLifecycleFailure(UUID promotionId, String action, RuntimeException failure) {
        LOG.error("promotion lifecycle escalation: promotionId={}, action={}", promotionId, action, failure);
    }
    @Override public void reportAuditGap(UUID promotionId, String action, RuntimeException failure) {
        LOG.error("promotion audit compliance escalation: promotionId={}, action={}", promotionId, action, failure);
    }
}
