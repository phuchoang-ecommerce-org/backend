package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import org.mapstruct.Mapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.CredentialHash;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Maps the Account aggregate's persistence state without exposing JPA concerns
 * to the domain.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AccountJpaMapper {

  AccountEntity toEntity(AccountState source);

  AccountState toState(AccountEntity source);

  default AccountEntity toEntity(Account account) {
    return toEntity(AccountState.from(account));
  }

  @AfterMapping
  default void initializeAuditTimestamps(AccountState source, @MappingTarget AccountEntity target) {
    target.initializeAuditTimestamps(source.createdAt());
  }

  default Account toDomain(AccountEntity entity, Set<RoleCode> roles) {
    AccountState state = toState(entity);
    return Account.reconstitute(state.id(), mapEmailAddress(state.email()), mapEmailAddress(state.pendingEmail()),
        mapCredentialHash(state.credentialHash()),
        state.displayName(), state.status(), state.verificationStatus(), state.verifiedAt(), state.lastLoginAt(),
        state.failedLoginCount(), state.version(), roles, state.createdAt());
  }

  default EmailAddress mapEmailAddress(String source) {
    return source == null ? null : new EmailAddress(source);
  }

  default CredentialHash mapCredentialHash(String source) {
    return source == null ? null : new CredentialHash(source);
  }

  record AccountState(UUID id, String email, String pendingEmail, String credentialHash, String displayName,
      org.phuchoang.ecp.identity.internal.domain.model.AccountStatus status,
      org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus verificationStatus,
      Instant verifiedAt, Instant lastLoginAt, int failedLoginCount, long version, Instant createdAt) {
    static AccountState from(Account account) {
      return new AccountState(account.id(), account.email().value(),
          account.pendingEmail() == null ? null : account.pendingEmail().value(), account.credentialHash().value(),
          account.displayName(), account.status(), account.verificationStatus(), account.verifiedAt(),
          account.lastLoginAt(),
          account.failedLoginCount(), account.version(), account.createdAt());
    }
  }
}
