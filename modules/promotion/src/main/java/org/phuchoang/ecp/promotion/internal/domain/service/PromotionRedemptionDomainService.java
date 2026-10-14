package org.phuchoang.ecp.promotion.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;
import org.phuchoang.ecp.promotion.internal.domain.repository.PromotionRepository;

/** Invokes the Promotion root's locked redemption operation. */
@Service
public final class PromotionRedemptionDomainService {

    private final PromotionRepository promotions;

    public PromotionRedemptionDomainService(PromotionRepository promotions) {
        this.promotions = promotions;
    }

    public Promotion.Redemption redeem(Promotion.RedeemPromotion request) {
        try {
            return Promotion.redeem(request, promotions);
        } catch (Promotion.RedemptionUnavailableException unavailable) {
            throw new PromotionRedemptionUnavailableException();
        }
    }

    public static final class PromotionRedemptionUnavailableException extends RuntimeException {
    }
}
