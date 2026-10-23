package org.phuchoang.ecp.promotion.internal.application.status.command;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.promotion.internal.domain.model.PromotionStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Intent to apply or schedule an operator-approved promotion lifecycle
 * transition.
 */
public record SetPromotionStatusCommand(IdentityActor caller, UUID correlationId, UUID promotionId,
        PromotionStatus status, String reason, Instant effectiveAt) {
    public static SetPromotionStatusCommand fromStatusName(IdentityActor caller, UUID correlationId, UUID promotionId,
            String status, String reason, Instant effectiveAt) {
        return new SetPromotionStatusCommand(caller, correlationId, promotionId,
                PromotionStatus.valueOf(status), reason, effectiveAt);
    }

    public SetPromotionStatusCommand {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(promotionId, "promotionId");
        Objects.requireNonNull(status, "status");
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("A promotion status reason is required.");
    }
}
