package org.phuchoang.ecp.promotion.internal.application.validate.query;

import java.math.BigDecimal;
import java.util.UUID;

/** Constant-shape, observational outcome for voucher applicability. */
public record VoucherValidation(boolean valid, UUID promotionId, String code, BigDecimal discountAmount,
        String currency) {

    public static VoucherValidation notValid() {
        return new VoucherValidation(false, null, null, null, null);
    }
}
