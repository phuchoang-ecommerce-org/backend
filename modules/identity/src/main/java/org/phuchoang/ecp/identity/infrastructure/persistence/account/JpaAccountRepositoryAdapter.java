package org.phuchoang.ecp.identity.infrastructure.persistence.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.phuchoang.ecp.identity.application.port.AccountRepository;
import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.CredentialHash;
import org.phuchoang.ecp.identity.domain.EmailAddress;
import org.phuchoang.ecp.identity.domain.RoleCode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapts {@link Account} to {@code identity_account} + {@code identity_account_role}. A duplicate
 * email surfaces as {@link AccountRepository.DuplicateEmailException}, translated from
 * {@code ux_identity_account_email}'s violation (`BR-CUS-01`) — never a preceding existence check.
 */
@Repository
class JpaAccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountRoleAssignments accountRoleAssignments;
    private final EntityManager entityManager;

    JpaAccountRepositoryAdapter(AccountJpaRepository accountJpaRepository,
            AccountRoleAssignments accountRoleAssignments, EntityManager entityManager) {
        this.accountJpaRepository = accountJpaRepository;
        this.accountRoleAssignments = accountRoleAssignments;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account registerNew(Account account) {
        // REQUIRES_NEW: a failed insert here must roll back only this transaction. Once a flush
        // fails, Spring/Hibernate mark the *current physical transaction* rollback-only regardless
        // of whether the Java exception is caught — catching it and continuing in the caller's own
        // transaction would make that transaction's later commit throw UnexpectedRollbackException.
        // A dedicated nested transaction is what lets RegisterAccountService catch
        // DuplicateEmailException and still commit its own (otherwise untouched) transaction.
        AccountEntity entity = new AccountEntity(account.id(), account.email().value(), pendingEmailValueOf(account),
            account.credentialHash().value(), account.displayName(), account.status(),
            account.verificationStatus(), account.verifiedAt(), account.lastLoginAt(),
            account.failedLoginCount(), account.version(), account.createdAt());
        try {
            entityManager.persist(entity);
            entityManager.flush();
            for (RoleCode role : account.roles()) {
                accountRoleAssignments.grant(entity.getId(), role);
            }
            return toDomain(entity, account.roles());
        } catch (PersistenceException e) {
            throw new DuplicateEmailException(e);
        }
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = new AccountEntity(account.id(), account.email().value(), pendingEmailValueOf(account),
            account.credentialHash().value(), account.displayName(), account.status(),
            account.verificationStatus(), account.verifiedAt(), account.lastLoginAt(),
            account.failedLoginCount(), account.version(), account.createdAt());
        AccountEntity saved = entityManager.merge(entity);
        return toDomain(saved, account.roles());
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return accountJpaRepository.findById(id)
            .map(entity -> toDomain(entity, accountRoleAssignments.rolesOf(entity.getId())));
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        return accountJpaRepository.findByEmailIgnoreCase(email.value())
            .map(entity -> toDomain(entity, accountRoleAssignments.rolesOf(entity.getId())));
    }

    private Account toDomain(AccountEntity entity, Set<RoleCode> roles) {
        EmailAddress pendingEmail = entity.getPendingEmail() == null ? null : new EmailAddress(entity.getPendingEmail());
        return Account.reconstitute(entity.getId(), new EmailAddress(entity.getEmail()), pendingEmail,
            new CredentialHash(entity.getCredentialHash()), entity.getDisplayName(), entity.getStatus(),
            entity.getVerificationStatus(), entity.getVerifiedAt(), entity.getLastLoginAt(),
            entity.getFailedLoginCount(), entity.getVersion(), roles, entity.getCreatedAt());
    }

    private static String pendingEmailValueOf(Account account) {
        return account.pendingEmail() == null ? null : account.pendingEmail().value();
    }
}
