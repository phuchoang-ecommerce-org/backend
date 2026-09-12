package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Raised when the caller's own password is changed (`UC-CUS-06`) or reset (`UC-CUS-07`). */
public record PasswordChanged(UUID accountId, Instant occurredAt) {
}
