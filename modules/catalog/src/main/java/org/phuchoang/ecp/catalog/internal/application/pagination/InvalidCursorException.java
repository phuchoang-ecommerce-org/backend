package org.phuchoang.ecp.catalog.internal.application.pagination;

/** Raised when a cursor is malformed, untrusted, or incompatible with its requested listing. */
public final class InvalidCursorException extends IllegalArgumentException {

    public InvalidCursorException() {
        super("Cursor is malformed or incompatible with this request.");
    }
}
