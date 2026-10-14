package org.phuchoang.ecp.promotion.internal.domain.policy;

import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Pure deterministic fallback for conflicting eligible promotions (BR-PRM-03). */
public final class PromotionStackingPolicy {
    public Promotion selectMostFavourable(List<Promotion> candidates, BigDecimal discountableAmount) {
        return candidates.stream().max(Comparator
            .comparing((Promotion promotion) -> promotion.calculateDiscount(discountableAmount))
            .thenComparing(Promotion::priority)
            .thenComparing(Promotion::id)).orElseThrow(() -> new IllegalArgumentException("No eligible promotion."));
    }
}
