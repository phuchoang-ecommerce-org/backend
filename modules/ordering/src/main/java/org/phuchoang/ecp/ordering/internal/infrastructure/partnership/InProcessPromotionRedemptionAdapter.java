package org.phuchoang.ecp.ordering.internal.infrastructure.partnership;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.application.place.command.PromotionRedemptionPort;
import org.phuchoang.ecp.promotion.api.PromotionFacade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Current in-process implementation of Ordering's binding redemption port. */
@Component
@RequiredArgsConstructor
class InProcessPromotionRedemptionAdapter implements PromotionRedemptionPort {
    private final PromotionFacade promotions;

    @Override
    public Discount redeem(UUID promotionId, UUID orderId, UUID customerId, BigDecimal subtotal) {
        var value = promotions.redeem(promotionId, orderId, customerId, subtotal);
        return new Discount(value.promotionId(), value.discountAmount(), value.currency());
    }
}
