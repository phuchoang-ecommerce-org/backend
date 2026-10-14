package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.promotion.internal.application.list.query.PromotionListItem;
import org.phuchoang.ecp.promotion.internal.application.list.query.PromotionListQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/** PostgreSQL adapter for the operator list read model. */
@Repository
@RequiredArgsConstructor
class JpaPromotionListQueryAdapter implements PromotionListQuery {

    private final PromotionJpaRepository promotions;
    private final PromotionListJpaMapper mapper;

    @Override
    public List<PromotionListItem> list(String status, int limit) {
        List<PromotionEntity> values = status == null || status.isBlank()
            ? promotions.findTop100ByOrderByCreatedAtDesc()
            : promotions.findTop100ByStatusOrderByCreatedAtDesc(status);
        return values.stream().limit(Math.max(1, Math.min(limit, 100))).map(mapper::listItem).toList();
    }
}
