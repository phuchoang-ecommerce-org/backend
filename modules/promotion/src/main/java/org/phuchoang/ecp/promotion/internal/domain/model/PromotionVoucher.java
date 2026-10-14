package org.phuchoang.ecp.promotion.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A generated secret voucher; single-use state is retained independently of the campaign cap. */
public record PromotionVoucher(UUID id, UUID promotionId, String code, boolean singleUse, Instant redeemedAt) {
    public PromotionVoucher {
        if (id == null || promotionId == null || code == null || code.isBlank()) {
            throw new IllegalArgumentException("A voucher requires an identity, promotion and code.");
        }
    }
    public boolean available() { return !singleUse || redeemedAt == null; }
}
