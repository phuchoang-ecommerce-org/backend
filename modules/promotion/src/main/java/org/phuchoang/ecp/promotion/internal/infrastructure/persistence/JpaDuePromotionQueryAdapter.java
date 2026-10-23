package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import org.phuchoang.ecp.promotion.internal.application.status.query.DuePromotionQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only due-work lookup; command handling reloads each authoritative
 * aggregate.
 */
@Repository
@lombok.RequiredArgsConstructor
class JpaDuePromotionQueryAdapter implements DuePromotionQuery {
    private final PromotionJpaRepository promotions;

    @Override
    public List<UUID> scheduledStatusChanges(Instant now, int limit) {
        return promotions.findTop100ByScheduledAtLessThanEqualOrderByScheduledAtAsc(now).stream()
                .limit(limit).map(PromotionEntity::getId).toList();
    }

    @Override
    public List<UUID> activations(Instant now, int limit) {
        return promotions.findTop100ByStatusAndValidFromLessThanEqualAndValidUntilGreaterThanOrderByValidFromAsc(
                "DRAFT", now, now).stream().limit(limit).map(PromotionEntity::getId).toList();
    }

    @Override
    public List<UUID> expirations(Instant now, int limit) {
        return promotions.findTop100ByStatusAndValidUntilLessThanEqualOrderByValidUntilAsc("ACTIVE", now).stream()
                .limit(limit).map(PromotionEntity::getId).toList();
    }
}
