package org.phuchoang.ecp.promotion.internal.application.generatevouchers.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.UUID;

/** Application input for generating promotion-owned voucher codes. */
public record GeneratePromotionVouchersCommand(IdentityActor caller, UUID correlationId, UUID promotionId, int count,
        boolean singleUse, String prefix) {
}
