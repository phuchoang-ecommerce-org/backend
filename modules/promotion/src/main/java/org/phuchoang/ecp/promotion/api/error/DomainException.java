package org.phuchoang.ecp.promotion.api.error;

/** Promotion's published, machine-readable exceptional outcome. */
public class DomainException extends RuntimeException {
    private final ErrorCode errorCode;
    public DomainException(ErrorCode errorCode, String detail) { super(detail); this.errorCode = errorCode; }
    public ErrorCode errorCode() { return errorCode; }
}
