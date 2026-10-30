package org.phuchoang.ecp.shipping.api.error;

/** Cross-cutting outcomes exposed by Shipping's public boundary. */
public enum GenErrorCode implements ErrorCode {
    NOT_FOUND("ECP-GEN-4040", 404, "Not found"),
    DEPENDENCY_UNAVAILABLE("ECP-GEN-5030", 503, "Dependency unavailable");

    private final String code;
    private final int httpStatus;
    private final String title;

    GenErrorCode(String code, int httpStatus, String title) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.title = title;
    }

    @Override public String code() { return code; }
    @Override public int httpStatus() { return httpStatus; }
    @Override public String title() { return title; }
}
