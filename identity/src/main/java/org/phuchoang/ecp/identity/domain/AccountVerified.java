package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Raised when an account's email address is proven (`UC-CUS-02`). */
public record AccountVerified(UUID accountId, String email, Instant occurredAt) {
}
