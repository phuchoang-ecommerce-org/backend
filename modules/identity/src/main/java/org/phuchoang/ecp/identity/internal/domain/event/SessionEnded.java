package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** Raised on `logOut`/`endAllOwnSessions` (`UC-CUS-04`), an `UC-AUD-01` audited action — stubbed as a log line this sprint. */
@DomainEvent
public record SessionEnded(UUID accountId, boolean allSessions, Instant occurredAt) {
}
