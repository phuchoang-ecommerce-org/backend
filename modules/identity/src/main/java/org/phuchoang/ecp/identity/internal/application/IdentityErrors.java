package org.phuchoang.ecp.identity.internal.application;

import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;

/**
 * The one place identity's security-sensitive response semantics are decided, so they can be
 * audited together. Several distinct failures deliberately share one code and one message so the
 * caller cannot tell them apart (`BR-CUS-04`, `Integration Contract.md` §2.1): an unknown email
 * from a wrong password, an expired link from a fabricated one, another customer's address from
 * a missing one. Identity-specific by design — not a generic exception utility.
 */
public final class IdentityErrors {

    private IdentityErrors() {
    }

    /** `UC-CUS-03` E1/E3 — unknown account, wrong password, and suspended account are identical. */
    public static DomainException invalidCredentials() {
        return new DomainException(GenErrorCode.NOT_AUTHENTICATED, "Email or password is incorrect.");
    }

    /** `UC-CUS-05` — invalid or expired refresh token; the chain is left intact. */
    public static DomainException refreshTokenRejected() {
        return new DomainException(GenErrorCode.REFRESH_TOKEN_REJECTED, "Refresh token is invalid or expired.");
    }

    /** `UC-CUS-05` — reuse of an already-rotated refresh token (or a lost rotation race). */
    public static DomainException refreshTokenReused() {
        return new DomainException(GenErrorCode.REFRESH_TOKEN_REJECTED,
            "Refresh token has already been used; the session chain has been ended.");
    }

    /** `UC-CUS-02` E1/E2 — expired, unrecognised, consumed, raced, or orphaned verification link. */
    public static DomainException invalidVerificationLink() {
        return new DomainException(GenErrorCode.NOT_FOUND, "The verification link is not valid.");
    }

    /** `UC-CUS-07` E1/E2 — expired, unrecognised, consumed, raced, orphaned, or inactive-account reset link. */
    public static DomainException invalidResetLink() {
        return new DomainException(GenErrorCode.NOT_FOUND, "The reset link is not valid.");
    }

    /** The caller's own account row is gone — reported as not found, never as an authorization failure. */
    public static DomainException accountNotFound() {
        return new DomainException(GenErrorCode.NOT_FOUND, "Account not found.");
    }

    /** Absent address and another customer's address are identical (`Integration Contract.md` §2.1). */
    public static DomainException addressNotFound() {
        return new DomainException(GenErrorCode.NOT_FOUND, "Address not found.");
    }

    public static DomainException passwordPolicyViolation(String violation) {
        return new DomainException(GenErrorCode.VALIDATION_FAILED, violation);
    }

    /** `UC-CUS-06` E1. */
    public static DomainException currentPasswordIncorrect() {
        return new DomainException(GenErrorCode.VALIDATION_FAILED, "Current password is incorrect.");
    }

    /** `UC-CUS-06` E3. */
    public static DomainException passwordUnchanged() {
        return new DomainException(GenErrorCode.VALIDATION_FAILED,
            "New password must be different from the current password.");
    }

    /** `UC-CUS-08` E1 — reported as "cannot be used", never "already registered". */
    public static DomainException emailUnavailable() {
        return new DomainException(GenErrorCode.VALIDATION_FAILED, "This email address cannot be used.");
    }
}
