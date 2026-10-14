package org.phuchoang.ecp.promotion.internal.application.status.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionStatus;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionLifecycleDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Authorizes lifecycle changes; the aggregate remains the transition authority. */
@Service
@RequiredArgsConstructor
public class SetPromotionStatusService {
    private static final String SET_PROMOTION_STATUS = "setPromotionStatus";
    private final PromotionLifecycleDomainService promotions;
    private final IdentityAuthorization authorization;
    private final AuditTrail audit;
    private final PromotionLifecycleFailureReporter failures;
    private final Clock clock;

    @Transactional
    public PromotionStatusResult set(SetPromotionStatusCommand command) {
        try {
            authorization.assertAuthorized(command.caller(), SET_PROMOTION_STATUS);
        } catch (RuntimeException denied) {
            recordDenied(command);
            throw denied;
        }
        Instant now = clock.instant();
        Promotion changed = command.effectiveAt() != null && command.effectiveAt().isAfter(now)
            ? promotions.scheduleStatus(command.promotionId(), command.status(), command.effectiveAt(), now)
            : promotions.changeStatus(command.promotionId(), command.status(), now);
        recordAfterCommit(changed, command.caller(), command.correlationId(), command.reason(), "promotionStatusChanged");
        return PromotionStatusResult.from(changed);
    }

    @Transactional
    public void activateWindow(UUID promotionId, Instant now) {
        Promotion changed = promotions.activate(promotionId, now);
        recordAfterCommit(changed, null, null, "validity window opened", "promotionActivated");
    }

    @Transactional
    public void expireWindow(UUID promotionId, Instant now) {
        Promotion changed = promotions.expire(promotionId, now);
        recordAfterCommit(changed, null, null, "validity window ended", "promotionExpired");
    }

    @Transactional
    public void applyScheduled(UUID promotionId, Instant now) {
        Promotion changed = promotions.applyScheduledStatus(promotionId, now);
        recordAfterCommit(changed, null, null, "scheduled status applied", "promotionStatusChanged");
    }

    private void recordDenied(SetPromotionStatusCommand command) {
        audit.record(new AuditRecord(UUID.randomUUID(), command.caller().accountId(), firstRole(command.caller()),
            "promotionStatusChangeDenied", "promotion", command.promotionId(), "promotion", Map.of(),
            Map.of("status", command.status().name()), command.reason(), command.correlationId(), clock.instant(), null, false));
    }

    private void recordAfterCommit(Promotion promotion, IdentityActor caller, UUID correlationId, String reason, String action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    audit.record(new AuditRecord(UUID.randomUUID(), caller == null ? null : caller.accountId(),
                        caller == null ? null : firstRole(caller), action, "promotion", promotion.id(), "promotion", Map.of(),
                        Map.of("status", promotion.status().name(), "usageCount", promotion.usageCount()), reason, correlationId,
                        clock.instant(), caller == null ? "promotion-lifecycle-scheduler" : null, caller == null));
                } catch (RuntimeException failure) {
                    failures.reportAuditGap(promotion.id(), action, failure);
                }
            }
        });
    }

    private static String firstRole(IdentityActor caller) {
        return caller.roles().stream().sorted().findFirst().orElse(null);
    }
}
