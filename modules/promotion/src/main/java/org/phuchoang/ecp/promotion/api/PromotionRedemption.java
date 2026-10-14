package org.phuchoang.ecp.promotion.api;

import java.math.BigDecimal;
import java.util.UUID;

/** Binding outcome used only by the order-placement partnership; it has no HTTP endpoint. */
public record PromotionRedemption(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountAmount,
        String currency) { }
