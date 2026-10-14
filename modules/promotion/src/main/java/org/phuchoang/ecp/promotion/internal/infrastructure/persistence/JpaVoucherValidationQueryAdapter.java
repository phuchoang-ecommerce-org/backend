package org.phuchoang.ecp.promotion.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.promotion.internal.application.validate.query.VoucherValidation;
import org.phuchoang.ecp.promotion.internal.application.validate.query.VoucherValidationQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** PostgreSQL implementation of the observational voucher applicability query. */
@Repository
@RequiredArgsConstructor
class JpaVoucherValidationQueryAdapter implements VoucherValidationQuery {

    private final PromotionJpaRepository promotions;
    private final PromotionVoucherJpaRepository vouchers;
    private final PromotionRedemptionJpaRepository redemptions;

    @Override
    public VoucherValidation validate(UUID customerId, String code, BigDecimal discountableAmount, Instant now) {
        PromotionEntity promotion = promotions.findByCode(code).orElseGet(() -> vouchers.findByCode(code)
            .filter(voucher -> !voucher.isSingleUse() || voucher.getRedeemedAt() == null)
            .flatMap(voucher -> promotions.findById(voucher.getPromotionId()))
            .orElse(null));
        if (promotion == null || !eligible(promotion, discountableAmount, now)
                || redemptions.countByPromotionIdAndCustomerId(promotion.getId(), customerId) >= promotion.getPerCustomerLimit()) {
            return VoucherValidation.notValid();
        }
        return new VoucherValidation(true, promotion.getId(), promotion.getCode(), calculateDiscount(promotion, discountableAmount),
            promotion.getCurrency());
    }

    private static boolean eligible(PromotionEntity promotion, BigDecimal discountableAmount, Instant now) {
        if (!"ACTIVE".equals(promotion.getStatus()) || now.isBefore(promotion.getValidFrom())
                || !now.isBefore(promotion.getValidUntil()) || promotion.getUsageCount() >= promotion.getUsageLimit()) {
            return false;
        }
        if (discountableAmount == null || discountableAmount.signum() < 0) {
            throw new IllegalArgumentException("Discountable amount must not be negative.");
        }
        return promotion.getMinOrderAmount() == null || discountableAmount.compareTo(promotion.getMinOrderAmount()) >= 0;
    }

    private static BigDecimal calculateDiscount(PromotionEntity promotion, BigDecimal discountableAmount) {
        BigDecimal proposed = switch (promotion.getDiscountType()) {
            case "PERCENTAGE" -> discountableAmount.multiply(promotion.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            case "FIXED_AMOUNT" -> promotion.getDiscountValue();
            case "FREE_SHIPPING" -> discountableAmount;
            default -> throw new IllegalStateException("Unknown promotion discount type: " + promotion.getDiscountType());
        };
        if (promotion.getMaxDiscountAmount() != null) {
            proposed = proposed.min(promotion.getMaxDiscountAmount());
        }
        return proposed.min(discountableAmount).setScale(4, RoundingMode.HALF_UP);
    }
}
