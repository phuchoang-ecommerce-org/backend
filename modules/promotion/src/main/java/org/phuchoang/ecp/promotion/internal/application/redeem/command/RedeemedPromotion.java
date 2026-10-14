package org.phuchoang.ecp.promotion.internal.application.redeem.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Command-owned outcome of a successful authoritative redemption. */
public record RedeemedPromotion(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountAmount,
        String currency) {
}
