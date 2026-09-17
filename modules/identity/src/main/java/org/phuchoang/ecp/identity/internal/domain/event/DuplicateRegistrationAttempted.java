package org.phuchoang.ecp.identity.internal.domain.event;

import org.jmolecules.event.annotation.DomainEvent;
import java.time.Instant;

/**
 * Raised instead of {@link AccountRegistered} when the address was already registered
 * (`UC-CUS-01` E1, `BR-CUS-04`). The response given to the caller is identical either way — this
 * event is what should eventually dispatch a "someone tried to register with your address" mail
 * to the real owner (stubbed as a log line this sprint; the `notification` module doesn't exist
 * yet — see Sprint 03 Review Notes).
 */
@DomainEvent
public record DuplicateRegistrationAttempted(String email, Instant occurredAt) {
}
