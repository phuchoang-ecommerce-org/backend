package org.phuchoang.ecp.review.api;

import org.phuchoang.ecp.sharedkernel.api.error.ErrorCode;

/**
 * The {@code REV} domain (Error Codes.md §3.11). Unbypassable by any role — the verified-purchase
 * read model is eventually consistent, so the response tells the caller to retry shortly rather than
 * that they never bought the product.
 */
public enum ReviewErrorCode implements ErrorCode {

    NOT_VERIFIED_BUYER("ECP-REV-4030", 403, "Not a verified buyer of this product");

    private final String code;
    private final int httpStatus;
    private final String title;

    ReviewErrorCode(String code, int httpStatus, String title) {
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
