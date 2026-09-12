package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a password-reset token is issued for a registered address (`UC-CUS-07`). Never
 * raised for an unregistered address — `BR-CUS-04` non-disclosure holds by there being no event
 * to log, not by suppressing one after the fact.
 */
public record PasswordResetRequested(UUID accountId, String email, String rawToken, Instant occurredAt) {
}
