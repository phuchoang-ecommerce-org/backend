package org.phuchoang.ecp.identity.internal.domain.model;

import org.phuchoang.ecp.identity.internal.domain.event.AccountProfileUpdated;
import org.phuchoang.ecp.identity.internal.domain.event.AccountVerified;
import org.phuchoang.ecp.identity.internal.domain.event.EmailChangeRequested;
import org.phuchoang.ecp.identity.internal.domain.event.PasswordChanged;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** L1 — no Spring context. `BR-CUS-01` (uniqueness) is a database concern, exercised at L4 instead. */
class AccountTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void registerCreatesAnUnverifiedActiveCustomerAccount_BR_CUS_02() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);

        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.verificationStatus()).isEqualTo(VerificationStatus.UNVERIFIED);
        assertThat(account.isVerified()).isFalse();
        assertThat(account.roles()).containsExactly(RoleCode.CUSTOMER);
    }

    @Test
    void verifyMarksTheAccountVerifiedAndRaisesAccountVerified_BR_CUS_02() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);

        account.verify(clock);

        assertThat(account.isVerified()).isTrue();
        assertThat(account.verifiedAt()).isEqualTo(Instant.now(clock));
        assertThat(account.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccountVerified.class);
    }

    @Test
    void verifyIsIdempotentForAnAlreadyVerifiedAccount_UC_CUS_02_A1() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);
        account.verify(clock);
        account.pullDomainEvents(); // drain the first verification's event

        account.verify(clock); // A1 — following the link twice

        assertThat(account.pullDomainEvents()).isEmpty();
    }

    @Test
    void recordFailedLoginIncrementsTheCounter() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);

        account.recordFailedLogin();
        account.recordFailedLogin();

        assertThat(account.failedLoginCount()).isEqualTo(2);
    }

    @Test
    void recordSuccessfulLoginResetsTheFailedCounterAndStampsLastLoginAt() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);
        account.recordFailedLogin();

        account.recordSuccessfulLogin(clock);

        assertThat(account.failedLoginCount()).isZero();
        assertThat(account.lastLoginAt()).isEqualTo(Instant.now(clock));
    }

    @Test
    void changePasswordReplacesTheHashAndRaisesPasswordChanged_UC_CUS_06() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("old-hash"), "Guest", clock);

        account.changePassword(new CredentialHash("new-hash"), clock);

        assertThat(account.credentialHash().value()).isEqualTo("new-hash");
        assertThat(account.pullDomainEvents()).hasSize(1).first().isInstanceOf(PasswordChanged.class);
    }

    @Test
    void requestEmailChangeHoldsTheNewAddressPendingWithoutTouchingEmail_UC_CUS_08_A1() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);

        account.requestEmailChange(new EmailAddress("new@example.com"), clock);

        assertThat(account.email().value()).isEqualTo("guest@example.com");
        assertThat(account.pendingEmail().value()).isEqualTo("new@example.com");
        assertThat(account.pullDomainEvents()).hasSize(2)
            .anyMatch(AccountProfileUpdated.class::isInstance)
            .anyMatch(EmailChangeRequested.class::isInstance);
    }

    @Test
    void confirmPendingEmailPromotesThePendingAddressAndClearsIt_UC_CUS_08_A1() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);
        account.requestEmailChange(new EmailAddress("new@example.com"), clock);
        account.pullDomainEvents();

        account.confirmPendingEmail(clock);

        assertThat(account.email().value()).isEqualTo("new@example.com");
        assertThat(account.pendingEmail()).isNull();
        assertThat(account.pullDomainEvents()).hasSize(1).first().isInstanceOf(AccountProfileUpdated.class);
    }

    @Test
    void confirmPendingEmailIsANoOpWhenNothingIsPending() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);

        account.confirmPendingEmail(clock);

        assertThat(account.email().value()).isEqualTo("guest@example.com");
        assertThat(account.pullDomainEvents()).isEmpty();
    }

    @Test
    void updateDisplayNameLeavesItUnchangedWhenNull_UC_CUS_08_A2() {
        Account account = Account.register(UUID.randomUUID(), new EmailAddress("guest@example.com"),
            new CredentialHash("hashed"), "Guest", clock);

        account.updateDisplayName(null);

        assertThat(account.displayName()).isEqualTo("Guest");
    }
}
