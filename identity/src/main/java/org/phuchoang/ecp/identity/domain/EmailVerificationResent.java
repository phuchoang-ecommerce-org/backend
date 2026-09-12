package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Raised on `resendEmailVerification` (`UC-CUS-02` A2). Carries the raw token for the same stubbed-notification reason as {@link AccountRegistered}. */
public record EmailVerificationResent(UUID accountId, String email, String verificationToken, Instant occurredAt) {
}
