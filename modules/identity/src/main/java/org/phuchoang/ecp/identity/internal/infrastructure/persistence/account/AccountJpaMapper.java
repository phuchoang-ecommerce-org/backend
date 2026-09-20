package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Maps the Account aggregate's persistence state without exposing JPA concerns to the domain. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AccountJpaMapper {

    AccountEntity toEntity(AccountState source);

    default AccountEntity toEntity(Account account) {
        AccountEntity entity = toEntity(AccountState.from(account));
        entity.initializeAuditTimestamps(account.createdAt());
        return entity;
    }

    default Account toDomain(AccountEntity entity, Set<RoleCode> roles) {
        EmailAddress pendingEmail = entity.getPendingEmail() == null ? null : new EmailAddress(entity.getPendingEmail());
        return Account.reconstitute(entity.getId(), new EmailAddress(entity.getEmail()), pendingEmail,
            new CredentialHash(entity.getCredentialHash()), entity.getDisplayName(), entity.getStatus(),
            entity.getVerificationStatus(), entity.getVerifiedAt(), entity.getLastLoginAt(),
            entity.getFailedLoginCount(), entity.getVersion(), roles, entity.getCreatedAt());
    }

    record AccountState(UUID id, String email, String pendingEmail, String credentialHash, String displayName,
                        org.phuchoang.ecp.identity.internal.domain.model.AccountStatus status,
                        org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus verificationStatus,
                        Instant verifiedAt, Instant lastLoginAt, int failedLoginCount, long version, Instant createdAt) {
        static AccountState from(Account account) {
            return new AccountState(account.id(), account.email().value(),
                account.pendingEmail() == null ? null : account.pendingEmail().value(), account.credentialHash().value(),
                account.displayName(), account.status(), account.verificationStatus(), account.verifiedAt(), account.lastLoginAt(),
                account.failedLoginCount(), account.version(), account.createdAt());
        }
    }
}
