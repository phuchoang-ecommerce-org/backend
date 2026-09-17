package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** Raised on a successful `logIn` (`UC-CUS-03`), an `UC-AUD-01` audited action — stubbed as a log line this sprint. */
@DomainEvent
public record SessionEstablished(UUID accountId, boolean restricted, Instant occurredAt) {
}
