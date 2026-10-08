package org.phuchoang.ecp.catalog.internal.application.error;

/** A Catalog use-case failure translated to the public error contract by an API adapter. */
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
