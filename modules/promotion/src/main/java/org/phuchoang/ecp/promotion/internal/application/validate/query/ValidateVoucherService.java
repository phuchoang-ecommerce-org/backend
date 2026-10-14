package org.phuchoang.ecp.promotion.internal.application.validate.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

/** Executes the observational voucher validation query without loading a domain aggregate. */
@Service
@RequiredArgsConstructor
public class ValidateVoucherService {

    private final VoucherValidationQuery vouchers;
    private final Clock clock;

    @Transactional(readOnly = true)
    public VoucherValidation validate(UUID customerId, String code, BigDecimal discountableAmount) {
        if (code == null || code.isBlank()) {
            return VoucherValidation.notValid();
        }
        return vouchers.validate(customerId, code.trim(), discountableAmount, clock.instant());
    }
}
