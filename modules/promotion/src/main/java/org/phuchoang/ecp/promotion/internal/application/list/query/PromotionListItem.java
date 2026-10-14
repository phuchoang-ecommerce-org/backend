package org.phuchoang.ecp.promotion.internal.application.list.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read model for the operator promotion list. */
public record PromotionListItem(UUID id, String code, String name, String description, String discountType,
        BigDecimal discountValue, String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
        Instant validFrom, Instant validUntil, int usageLimit, int usageCount, int perCustomerLimit, int priority,
        boolean stackable, String status) {
}
