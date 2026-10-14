package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

interface PromotionJpaRepository extends JpaRepository<PromotionEntity, UUID> {
    Optional<PromotionEntity> findByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<PromotionEntity> findLockedById(UUID id);
    java.util.List<PromotionEntity> findTop100ByStatusOrderByCreatedAtDesc(String status);
    java.util.List<PromotionEntity> findTop100ByOrderByCreatedAtDesc();
}
