package org.phuchoang.ecp.promotion.api;

import java.math.BigDecimal;
import java.util.UUID;

/** Deliberately constant-shape external outcome for voucher-code validation. */
public record VoucherValidationResult(boolean valid, UUID promotionId, String code, BigDecimal discountAmount,
        String currency) {
    public static VoucherValidationResult notValid() { return new VoucherValidationResult(false, null, null, null, null); }
}
