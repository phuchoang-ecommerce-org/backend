package org.phuchoang.ecp.promotion.internal.application.status.command;

import org.phuchoang.ecp.promotion.internal.domain.model.Promotion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Authoritative fields available immediately after a lifecycle command. */
public record PromotionStatusResult(UUID id, String code, String name, String description, String discountType,
        BigDecimal discountValue, String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
        Instant validFrom, Instant validUntil, int usageLimit, int usageCount, int perCustomerLimit, int priority,
        boolean stackable, String status) {
    static PromotionStatusResult from(Promotion promotion) {
        return new PromotionStatusResult(promotion.id(), promotion.code(), promotion.name(), promotion.description(),
            promotion.discountType().name(), promotion.discountValue(), promotion.currency(), promotion.maxDiscountAmount(),
            promotion.minOrderAmount(), promotion.validFrom(), promotion.validUntil(), promotion.usageLimit(),
            promotion.usageCount(), promotion.perCustomerLimit(), promotion.priority(), promotion.stackable(),
            promotion.status().name());
    }
}
