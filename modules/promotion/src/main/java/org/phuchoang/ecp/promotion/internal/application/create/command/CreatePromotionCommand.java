package org.phuchoang.ecp.promotion.internal.application.create.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Application input for configuring a promotion. */
public record CreatePromotionCommand(IdentityActor caller, UUID correlationId, String code, String name,
        String description, String discountType, BigDecimal discountValue, String currency, BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount, Instant validFrom, Instant validUntil, Integer usageLimit, Integer perCustomerLimit,
        int priority, boolean stackable) {
}
