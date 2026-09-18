package org.phuchoang.ecp.identity.internal.application.profile;

import org.phuchoang.ecp.identity.internal.domain.model.Account;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

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
    Instant createdAt) {

    public static AccountSummary of(Account account) {
        return new AccountSummary(
            account.id().toString(),
            account.email().value(),
            account.displayName(),
            account.status().name(),
            account.verificationStatus().name(),
            account.pendingEmail() == null ? null : account.pendingEmail().value(),
            account.roles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()),
            account.verifiedAt(),
            account.lastLoginAt(),
            account.createdAt());
    }
}
