package org.phuchoang.ecp.promotion.api;

import java.math.BigDecimal;
import java.time.Instant;

/** Transport-neutral command input for creating the bounded promotion configuration. */
public record PromotionWrite(String code, String name, String description, String discountType, BigDecimal discountValue,
        String currency, BigDecimal maxDiscountAmount, BigDecimal minOrderAmount, Instant validFrom, Instant validUntil,
        Integer usageLimit, Integer perCustomerLimit, int priority, boolean stackable) { }
