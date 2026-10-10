package org.phuchoang.ecp.identity.internal.application.profile;

import java.time.Instant;
import java.util.Set;

/**
 * A caller-facing snapshot of an {@link Account} — plain data only, so {@code identity.api} can
 * depend on it without depending on the domain package
 * (`apiDoesNotDependOnDomainOrInfrastructureInternals`, `Module Dependency Diagram.md` §6).
 */
public record AccountSummary(
    String id,
    String email,
    String displayName,
    String status,
    String verificationStatus,
    String pendingEmail,
    Set<String> roles,
    Instant verifiedAt,
    Instant lastLoginAt,
    Instant createdAt) { }
