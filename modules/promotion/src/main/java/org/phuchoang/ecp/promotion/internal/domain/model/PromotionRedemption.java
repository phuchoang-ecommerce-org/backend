package org.phuchoang.ecp.promotion.internal.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable commercial fact recorded only after a promotion slot is claimed. */
public record PromotionRedemption(UUID id, UUID promotionId, UUID orderId, UUID customerId,
        BigDecimal discountAmount, String currency, String voucherCode, Instant redeemedAt) {
    public PromotionRedemption {
        if (id == null || promotionId == null || orderId == null || customerId == null || redeemedAt == null
                || discountAmount == null || discountAmount.signum() < 0 || currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("A promotion redemption requires complete non-negative monetary evidence.");
        }
    }
}
