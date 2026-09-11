package org.phuchoang.ecp.ordering.api;

import org.phuchoang.ecp.sharedkernel.api.ErrorCode;

/**
 * The {@code ORD} domain (Error Codes.md §3.7).
 */
public enum OrderErrorCode implements ErrorCode {

    IDEMPOTENCY_KEY_MISSING("ECP-ORD-4001", 400, "Idempotency-Key required"),
    IDEMPOTENCY_KEY_REUSED("ECP-ORD-4090", 409, "Idempotency-Key reused with a different request body"),
    TRANSITION_NOT_PERMITTED("ECP-ORD-4091", 409, "Order state transition not permitted"),
    CART_NOT_PURCHASABLE("ECP-ORD-4220", 422, "Cart contains no purchasable line");

    private final String code;
    private final int httpStatus;
    private final String title;

    OrderErrorCode(String code, int httpStatus, String title) {
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
