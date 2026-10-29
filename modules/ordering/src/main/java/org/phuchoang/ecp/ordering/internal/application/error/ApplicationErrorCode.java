package org.phuchoang.ecp.ordering.internal.application.error;

/** Error semantics owned by Ordering application use cases, independent of API representation. */
public enum ApplicationErrorCode {
    NOT_FOUND,
    FORBIDDEN,
    IDEMPOTENCY_KEY_MISSING,
    IDEMPOTENCY_KEY_REUSED,
    CONFIRMATION_REQUIRED
}
