package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** Raised when the caller's own password is changed (`UC-CUS-06`) or reset (`UC-CUS-07`). */
@DomainEvent
public record PasswordChanged(UUID accountId, Instant occurredAt) {
}
