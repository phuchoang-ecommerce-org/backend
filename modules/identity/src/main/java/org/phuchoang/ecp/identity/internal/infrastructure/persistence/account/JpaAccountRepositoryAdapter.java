package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
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
    private final AccountJpaMapper mapper;

    JpaAccountRepositoryAdapter(AccountJpaRepository accountJpaRepository,
            AccountRoleAssignments accountRoleAssignments, EntityManager entityManager, AccountJpaMapper mapper) {
        this.accountJpaRepository = accountJpaRepository;
        this.accountRoleAssignments = accountRoleAssignments;
        this.entityManager = entityManager;
        this.mapper = mapper;
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
        AccountEntity entity = mapper.toEntity(account);
        try {
            entityManager.persist(entity);
            entityManager.flush();
            for (RoleCode role : account.roles()) {
                accountRoleAssignments.grant(entity.getId(), role);
            }
            return mapper.toDomain(entity, account.roles());
        } catch (PersistenceException e) {
            throw new DuplicateEmailException(e);
        }
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        AccountEntity saved = entityManager.merge(entity);
        return mapper.toDomain(saved, account.roles());
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return accountJpaRepository.findById(id)
            .map(entity -> mapper.toDomain(entity, accountRoleAssignments.rolesOf(entity.getId())));
    }

    @Override
    public Optional<Account> findByEmail(EmailAddress email) {
        return accountJpaRepository.findByEmailIgnoreCase(email.value())
            .map(entity -> mapper.toDomain(entity, accountRoleAssignments.rolesOf(entity.getId())));
    }
}
