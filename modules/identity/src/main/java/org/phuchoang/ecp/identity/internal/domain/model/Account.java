package org.phuchoang.ecp.identity.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.identity.internal.domain.event.AccountProfileUpdated;
import org.phuchoang.ecp.identity.internal.domain.event.AccountVerified;
import org.phuchoang.ecp.identity.internal.domain.event.EmailChangeRequested;
import org.phuchoang.ecp.identity.internal.domain.event.PasswordChanged;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The account aggregate root (Database.md §4.1, Domain Model.md §8.1). Testable with no Spring
 * context (Sprint 03 backlog) — this class imports nothing but the JDK, jMolecules' marker
 * annotation, and other {@code identity.domain} types.
 *
 * <p>The password never appears here in any form — only a {@link CredentialHash} — and hashing
 * itself is a {@code PasswordEncoder} port concern in application/infrastructure (`NFR-SEC-02`,
 * `NFR-SEC-07`). Verifying a supplied password against the stored hash is likewise an application
 * concern: this aggregate exposes the hash, it does not compare against one.
 */
@AggregateRoot
public final class Account {

    @Identity
    private final UUID id;
    private EmailAddress email;
    private EmailAddress pendingEmail;
    private CredentialHash credentialHash;
    private String displayName;
    private AccountStatus status;
    private VerificationStatus verificationStatus;
    private Instant verifiedAt;
    private Instant lastLoginAt;
    private int failedLoginCount;
    private long version;
    private final Set<RoleCode> roles;
    private final Instant createdAt;

    private final List<Object> domainEvents = new ArrayList<>();

    private Account(UUID id, EmailAddress email, EmailAddress pendingEmail, CredentialHash credentialHash,
            String displayName, AccountStatus status, VerificationStatus verificationStatus, Instant verifiedAt,
            Instant lastLoginAt, int failedLoginCount, long version, Set<RoleCode> roles, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.pendingEmail = pendingEmail;
        this.credentialHash = Objects.requireNonNull(credentialHash);
        this.displayName = displayName;
        this.status = Objects.requireNonNull(status);
        this.verificationStatus = Objects.requireNonNull(verificationStatus);
        this.verifiedAt = verifiedAt;
        this.lastLoginAt = lastLoginAt;
        this.failedLoginCount = failedLoginCount;
        this.version = version;
        this.roles = EnumSet.copyOf(roles);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /**
     * `UC-CUS-01` main scenario, step 4: a brand-new account, unverified, holding the Customer
     * role. No event is raised here — {@code AccountRegistered} also carries the verification
     * token (`VerificationTokenIssuing`, an application-layer concern the aggregate doesn't know
     * about), so the application service raises it once both exist.
     */
    public static Account register(UUID id, EmailAddress email, CredentialHash credentialHash,
            String displayName, Clock clock) {
        Instant now = Instant.now(clock);
        return new Account(id, email, null, credentialHash, displayName, AccountStatus.ACTIVE,
            VerificationStatus.UNVERIFIED, null, null, 0, 0L, EnumSet.of(RoleCode.CUSTOMER), now);
    }

    /** Reconstitutes a persisted account. No event is raised — rehydration is not a business event. */
    public static Account reconstitute(UUID id, EmailAddress email, EmailAddress pendingEmail,
            CredentialHash credentialHash, String displayName, AccountStatus status,
            VerificationStatus verificationStatus, Instant verifiedAt, Instant lastLoginAt, int failedLoginCount,
            long version, Set<RoleCode> roles, Instant createdAt) {
        return new Account(id, email, pendingEmail, credentialHash, displayName, status, verificationStatus,
            verifiedAt, lastLoginAt, failedLoginCount, version, roles, createdAt);
    }

    /**
     * `UC-CUS-02` main scenario. Idempotent (A1 — already verified is success, not an error): the
     * caller's goal already holds, and re-raising the event would be a false signal.
     */
    public void verify(Clock clock) {
        if (verificationStatus == VerificationStatus.VERIFIED) {
            return;
        }
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = Instant.now(clock);
        domainEvents.add(new AccountVerified(id, email.value(), Instant.now(clock)));
    }

    /**
     * `UC-CUS-06` step 4: replaces the stored hash. Session invalidation (`BR-CUS-03`) is a
     * {@code TokenStore} concern the application service handles itself — the aggregate
     * knows nothing about tokens.
     */
    public void changePassword(CredentialHash newCredentialHash, Clock clock) {
        this.credentialHash = Objects.requireNonNull(newCredentialHash);
        domainEvents.add(new PasswordChanged(id, Instant.now(clock)));
    }

    /**
     * `UC-CUS-08` main scenario, steps 2-4: whole-or-nothing profile update. {@code displayName}
     * is applied directly; {@code null} leaves it unchanged (`A2`).
     */
    public void updateDisplayName(String newDisplayName) {
        if (newDisplayName != null) {
            this.displayName = newDisplayName;
        }
    }

    /**
     * `UC-CUS-08` A1: a requested new address is held pending, never applied to {@code email}
     * directly, until proven via the same verification-token mechanism as initial registration.
     */
    public void requestEmailChange(EmailAddress newEmail, Clock clock) {
        this.pendingEmail = newEmail;
        domainEvents.add(new AccountProfileUpdated(id, Instant.now(clock)));
        domainEvents.add(new EmailChangeRequested(id, email.value(), newEmail.value(), Instant.now(clock)));
    }

    /** No pending email to confirm — a plain profile-field update (`A2`/no email change). */
    public void confirmProfileUpdate(Clock clock) {
        domainEvents.add(new AccountProfileUpdated(id, Instant.now(clock)));
    }

    /**
     * `UC-CUS-08` A1 completion: the verification token for {@code pendingEmail} was consumed.
     * Promotes it to {@code email} and clears the pending slot. Idempotent-safe: does nothing if
     * there is no pending email (the token would not have been issued).
     */
    public void confirmPendingEmail(Clock clock) {
        if (pendingEmail == null) {
            return;
        }
        this.email = pendingEmail;
        this.pendingEmail = null;
        domainEvents.add(new AccountProfileUpdated(id, Instant.now(clock)));
    }

    public void recordSuccessfulLogin(Clock clock) {
        this.lastLoginAt = Instant.now(clock);
        this.failedLoginCount = 0;
    }

    public void recordFailedLogin() {
        this.failedLoginCount++;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    /** Events raised since the last call, cleared as a side effect — pulled by the application layer after persisting. */
    public List<Object> pullDomainEvents() {
        List<Object> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public EmailAddress pendingEmail() {
        return pendingEmail;
    }

    public CredentialHash credentialHash() {
        return credentialHash;
    }

    public String displayName() {
        return displayName;
    }

    public AccountStatus status() {
        return status;
    }

    public VerificationStatus verificationStatus() {
        return verificationStatus;
    }

    public Instant verifiedAt() {
        return verifiedAt;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public int failedLoginCount() {
        return failedLoginCount;
    }

    public long version() {
        return version;
    }

    public Set<RoleCode> roles() {
        return Set.copyOf(roles);
    }

    public Instant createdAt() {
        return createdAt;
    }
}
