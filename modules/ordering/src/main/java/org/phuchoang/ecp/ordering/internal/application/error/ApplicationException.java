package org.phuchoang.ecp.ordering.internal.application.error;

/** An Ordering use-case failure translated to the public error contract by the API facade. */
public final class ApplicationException extends RuntimeException {

    private final ApplicationErrorCode errorCode;

    public ApplicationException(ApplicationErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ApplicationErrorCode errorCode() {
        return errorCode;
    }
}
