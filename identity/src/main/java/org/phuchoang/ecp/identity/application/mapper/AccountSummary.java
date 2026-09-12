package org.phuchoang.ecp.identity.application.mapper;

import java.time.Instant;
import java.util.Set;

/**
 * A caller-facing snapshot of an {@link org.phuchoang.ecp.identity.domain.Account} — plain data
 * only, so {@code identity.api} can depend on it without depending on the domain package
 * (`apiDoesNotDependOnDomainOrInfrastructureInternals`, `Module Dependency Diagram.md` §6).
 */
public record AccountSummary(
    String id,
    String email,
    String status,
    String verificationStatus,
    Set<String> roles,
    Instant verifiedAt,
    Instant lastLoginAt,
    Instant createdAt) {

    public static AccountSummary of(org.phuchoang.ecp.identity.domain.Account account) {
        return new AccountSummary(
            account.id().toString(),
            account.email().value(),
            account.status().name(),
            account.verificationStatus().name(),
            account.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            account.verifiedAt(),
            account.lastLoginAt(),
            account.createdAt());
    }
}
