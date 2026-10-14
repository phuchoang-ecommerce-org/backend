package org.phuchoang.ecp.promotion.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Operator-facing projection of a configured promotion. */
public record PromotionView(UUID id, String code, String name, String description, String discountType,
        BigDecimal discountValue, String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
        Instant validFrom, Instant validUntil, int usageLimit, int usageCount, int perCustomerLimit, int priority,
        boolean stackable, String status) { }
