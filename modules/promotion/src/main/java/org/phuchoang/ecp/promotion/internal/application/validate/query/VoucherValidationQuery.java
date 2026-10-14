package org.phuchoang.ecp.promotion.internal.application.validate.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Query port for the non-binding voucher applicability check. */
public interface VoucherValidationQuery {
    VoucherValidation validate(UUID customerId, String code, BigDecimal discountableAmount, Instant now);
}
