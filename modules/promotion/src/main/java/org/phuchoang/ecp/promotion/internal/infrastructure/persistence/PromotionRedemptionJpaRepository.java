package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PromotionRedemptionJpaRepository extends JpaRepository<PromotionRedemptionEntity, UUID> {
    long countByPromotionIdAndCustomerId(UUID promotionId, UUID customerId);
}
