package org.phuchoang.ecp.sharedkernel.api;

/** Raised when a cursor is malformed, untrusted, or incompatible with its requested listing. */
public final class InvalidCursorException extends DomainException {

    public InvalidCursorException() {
        super(GenErrorCode.VALIDATION_FAILED, "Cursor is malformed or incompatible with this request.");
    }
}
