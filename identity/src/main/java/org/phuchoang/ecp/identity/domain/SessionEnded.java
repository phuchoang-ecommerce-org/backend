package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/** Raised on `logOut`/`endAllOwnSessions` (`UC-CUS-04`), an `UC-AUD-01` audited action — stubbed as a log line this sprint. */
public record SessionEnded(UUID accountId, boolean allSessions, Instant occurredAt) {
}
