package org.phuchoang.ecp.cart.api;

import org.phuchoang.ecp.sharedkernel.api.ErrorCode;

/**
 * The {@code CRT} domain (Error Codes.md §3.6). {@code ECP-CRT-4090} is never silently capped — the
 * caller is told what is available and chooses (BR-CRT-02).
 */
public enum CartErrorCode implements ErrorCode {

    QUANTITY_EXCEEDS_STOCK("ECP-CRT-4090", 409, "Requested quantity exceeds available stock");

    private final String code;
    private final int httpStatus;
    private final String title;

    CartErrorCode(String code, int httpStatus, String title) {
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
