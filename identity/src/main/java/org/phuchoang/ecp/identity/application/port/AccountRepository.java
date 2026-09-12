package org.phuchoang.ecp.identity.application.port;

import org.phuchoang.ecp.identity.domain.Account;
import org.phuchoang.ecp.identity.domain.EmailAddress;

import java.util.Optional;
import java.util.UUID;

/**
 * The persistence port for {@link Account}. {@code save} on a duplicate email fails via the
 * database's own unique index (`ux_identity_account_email`, `BR-CUS-01`) rather than a preceding
 * {@code existsBy} check — a check-then-insert loses the race between two simultaneous
 * registrations of one address (Sequence/04-Identity.md §2).
 */
public interface AccountRepository {

    /**
     * Inserts a brand-new account in its own transaction (`REQUIRES_NEW`) — a duplicate-email
     * failure must roll back only this insert, never the caller's surrounding transaction (e.g.
     * `RegisterAccountService`'s, which still needs to commit its event publication normally
     * after catching {@link DuplicateEmailException}).
     */
    Account registerNew(Account account);

    /** Updates an existing account; participates in the caller's transaction. */
    Account save(Account account);

    Optional<Account> findById(UUID id);

    Optional<Account> findByEmail(EmailAddress email);

    /** Thrown by {@link #registerNew} when the email's unique index rejects the insert. */
    class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(Throwable cause) {
            super(cause);
        }
    }
}
