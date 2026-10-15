package org.phuchoang.ecp.ordering.internal.application.place.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Ordering-owned binding redemption port; checkout preview remains deliberately separate. */
public interface PromotionRedemptionPort {
    Discount redeem(UUID promotionId, UUID orderId, UUID customerId, BigDecimal subtotal);
    record Discount(UUID promotionId, BigDecimal amount, String currency) { }
}
