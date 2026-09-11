package org.phuchoang.ecp.promotion.api;

import org.phuchoang.ecp.sharedkernel.api.ErrorCode;

/**
 * The {@code PRM} domain (Error Codes.md §3.10). {@code ECP-PRM-4090} is, like
 * {@code ECP-INV-4091}, expected under peak load rather than exceptional — the visible surface of a
 * concurrency guarantee, not a client mistake.
 */
public enum PromotionErrorCode implements ErrorCode {

    USAGE_LIMIT_REACHED("ECP-PRM-4090", 409, "Promotion usage limit reached"),
    VOUCHER_NOT_APPLICABLE("ECP-PRM-4220", 422, "Voucher not applicable"),
    DISCOUNT_EXCEEDS_ORDER("ECP-PRM-4221", 422, "Discount exceeds discountable order value");

    private final String code;
    private final int httpStatus;
    private final String title;

    PromotionErrorCode(String code, int httpStatus, String title) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.title = title;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String title() {
        return title;
    }
}
