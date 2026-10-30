package org.phuchoang.ecp.shipping.api.error;

/** Contract-safe shipping failure mapped at the HTTP boundary. */
public class DomainException extends RuntimeException {
    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() { return errorCode; }
}
