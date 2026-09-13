package org.phuchoang.ecp.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * `UC-CUS-08` A1: a new address is requested. Two notifications follow from this one event — the
 * verification link to {@code newEmail}, and a heads-up to {@code oldEmail} so a hijacked session
 * cannot move an account away from its owner unseen.
 */
public record EmailChangeRequested(UUID accountId, String oldEmail, String newEmail, Instant occurredAt) {
}
