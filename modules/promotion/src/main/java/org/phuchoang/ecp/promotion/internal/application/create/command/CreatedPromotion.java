package org.phuchoang.ecp.promotion.internal.application.create.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Command-owned result for a newly configured promotion. */
public record CreatedPromotion(UUID id, String code, String name, String description, String discountType,
        BigDecimal discountValue, String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
        Instant validFrom, Instant validUntil, int usageLimit, int usageCount, int perCustomerLimit, int priority,
        boolean stackable, String status) {
}
