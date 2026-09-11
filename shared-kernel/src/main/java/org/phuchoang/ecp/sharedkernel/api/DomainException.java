package org.phuchoang.ecp.sharedkernel.api;

/**
 * The unchecked exception every domain-specific {@code 4xx}/{@code 5xx} outcome throws, carrying
 * the {@link ErrorCode} the wire-format advice maps to a problem+json body (Integration Contract.md
 * §4.1). No module has an application/domain layer yet, so nothing throws this today — it exists so
 * the advice has one exception type to handle, rather than a case per module added later.
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
