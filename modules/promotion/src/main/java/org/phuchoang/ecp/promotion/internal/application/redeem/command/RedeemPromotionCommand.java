package org.phuchoang.ecp.promotion.internal.application.redeem.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Application input for the binding promotion redemption performed during order placement. */
public record RedeemPromotionCommand(UUID promotionId, UUID orderId, UUID customerId, BigDecimal discountableAmount) {
}
