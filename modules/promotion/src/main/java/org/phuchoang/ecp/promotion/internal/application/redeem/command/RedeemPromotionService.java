package org.phuchoang.ecp.promotion.internal.application.redeem.command;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.promotion.internal.domain.service.PromotionRedemptionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Runs the promotion redemption transaction through the authoritative domain service. */
@Service
@RequiredArgsConstructor
public class RedeemPromotionService {

    private final PromotionRedemptionDomainService promotions;
    private final RedeemPromotionMapper mapper;
    private final Clock clock;

    @Transactional
    public RedeemedPromotion redeem(RedeemPromotionCommand command) {
        try {
            return mapper.redeemedPromotion(promotions.redeem(mapper.domainRequest(command, clock.instant())));
        } catch (PromotionRedemptionDomainService.PromotionRedemptionUnavailableException unavailable) {
            throw new PromotionUsageLimitReachedException();
        }
    }
}
