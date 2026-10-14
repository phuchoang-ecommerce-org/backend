package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PromotionVoucherJpaRepository extends JpaRepository<PromotionVoucherEntity, UUID> {
    Optional<PromotionVoucherEntity> findByCode(String code);
}
