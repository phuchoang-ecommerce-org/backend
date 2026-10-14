package org.phuchoang.ecp.promotion.api;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Published Promotion boundary for HTTP adapters and the Order-placement partnership. */
public interface PromotionFacade {
    PromotionView createPromotion(IdentityActor caller, UUID correlationId, PromotionWrite write);
    List<PromotionView> listPromotions(IdentityActor caller, String status, int limit);
    List<Voucher> generatePromotionVouchers(IdentityActor caller, UUID correlationId, UUID promotionId,
            int count, boolean singleUse, String prefix);
    PromotionView setPromotionStatus(IdentityActor caller, UUID correlationId, UUID promotionId, PromotionStatusWrite write);
    VoucherValidationResult validateVoucher(UUID customerId, String code, BigDecimal discountableAmount);
    PromotionRedemption redeem(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountableAmount);
}
