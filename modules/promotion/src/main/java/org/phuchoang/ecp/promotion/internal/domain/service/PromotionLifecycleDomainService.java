package org.phuchoang.ecp.promotion.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionStatus;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Coordinates lifecycle persistence while the Promotion root authorizes each transition. */
@Service
public final class PromotionLifecycleDomainService {
    private final PromotionRepository promotions;

    public PromotionLifecycleDomainService(PromotionRepository promotions) { this.promotions = promotions; }

    public Promotion changeStatus(UUID promotionId, PromotionStatus status, Instant now) {
        return Promotion.changeStatus(new Promotion.ChangeStatus(promotionId, status, now), promotions);
    }

    public Promotion scheduleStatus(UUID promotionId, PromotionStatus status, Instant effectiveAt, Instant now) {
        return Promotion.scheduleStatus(new Promotion.ScheduleStatus(promotionId, status, effectiveAt, now), promotions);
    }

    public List<Promotion> dueScheduledStatusChanges(Instant now, int limit) {
        return promotions.findDueScheduledStatusChanges(now, limit);
    }

    public List<Promotion> dueActivations(Instant now, int limit) { return promotions.findDueActivations(now, limit); }
    public List<Promotion> dueExpirations(Instant now, int limit) { return promotions.findDueExpirations(now, limit); }

    public Promotion applyScheduledStatus(UUID promotionId, Instant now) {
        return Promotion.applyScheduledStatus(new Promotion.ApplyScheduledStatus(promotionId, now), promotions);
    }

    public Promotion activate(UUID promotionId, Instant now) { return changeStatus(promotionId, PromotionStatus.ACTIVE, now); }
    public Promotion expire(UUID promotionId, Instant now) { return changeStatus(promotionId, PromotionStatus.EXPIRED, now); }

}
