package org.phuchoang.ecp.identity.internal.application.mapper;

import java.time.Instant;
import java.util.Set;

/**
 * A caller-facing snapshot of an {@link org.phuchoang.ecp.identity.internal.domain.model.Account} — plain data
 * only, so {@code identity.api} can depend on it without depending on the domain package
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
    Instant createdAt) {

    public static AccountSummary of(org.phuchoang.ecp.identity.internal.domain.model.Account account) {
        return new AccountSummary(
            account.id().toString(),
            account.email().value(),
            account.displayName(),
            account.status().name(),
            account.verificationStatus().name(),
            account.pendingEmail() == null ? null : account.pendingEmail().value(),
            account.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            account.verifiedAt(),
            account.lastLoginAt(),
            account.createdAt());
    }
}
