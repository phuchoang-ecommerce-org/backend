package org.phuchoang.ecp.payment.api;

import org.phuchoang.ecp.sharedkernel.api.error.ErrorCode;

/**
 * The {@code PAY} domain (Error Codes.md §3.8).
 */
public enum PaymentErrorCode implements ErrorCode {

    ATTEMPT_ALREADY_IN_FLIGHT("ECP-PAY-4090", 409, "A payment attempt is already in flight"),
    REFUND_EXCEEDS_CAPTURED("ECP-PAY-4220", 422, "Refund exceeds captured amount");

    private final String code;
    private final int httpStatus;
    private final String title;

    PaymentErrorCode(String code, int httpStatus, String title) {
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
