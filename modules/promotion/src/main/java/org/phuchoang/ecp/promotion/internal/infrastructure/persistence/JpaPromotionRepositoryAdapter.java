package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionRedemption;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionVoucher;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL adapter. Locked aggregate reads serialize redemption-slot claims.
 */
@Repository
@lombok.RequiredArgsConstructor
class JpaPromotionRepositoryAdapter implements PromotionRepository {
    private final PromotionJpaRepository promotions;
    private final PromotionRedemptionJpaRepository redemptions;
    private final PromotionVoucherJpaRepository vouchers;
    private final PromotionPersistenceMapper mapper;
    private final Clock clock;

    @Override
    public Optional<Promotion> findById(UUID id) {
        return promotions.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Promotion> findForRedemption(UUID id) {
        return promotions.findLockedById(id).map(mapper::toDomain);
    }

    @Override
    public long redemptionCountForCustomer(UUID promotionId, UUID customerId) {
        return redemptions.countByPromotionIdAndCustomerId(promotionId, customerId);
    }

    @Override
    public void save(Promotion promotion) {
        promotions.save(toEntity(promotion));
    }

    @Override
    public void recordRedemption(Promotion promotion, PromotionRedemption redemption) {
        promotions.save(toEntity(promotion));
        PromotionRedemptionEntity entity = mapper.toEntity(redemption);
        entity.setCreatedAt(clock.instant());
        redemptions.save(entity);
    }

    @Override
    public void appendVouchers(Promotion promotion, java.util.List<PromotionVoucher> values) {
        vouchers.saveAll(
                values.stream().map(mapper::toEntity).peek(entity -> entity.setCreatedAt(clock.instant())).toList());
    }

    private PromotionEntity toEntity(Promotion value) {
        PromotionEntity entity = promotions.findById(value.id()).orElseGet(PromotionEntity::new);
        boolean fresh = entity.getId() == null;
        mapper.updateEntity(value, entity);
        Instant now = clock.instant();
        if (fresh)
            entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
