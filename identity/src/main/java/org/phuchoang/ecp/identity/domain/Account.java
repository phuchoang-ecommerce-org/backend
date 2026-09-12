package org.phuchoang.ecp.identity.domain;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

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

    private Account(UUID id, EmailAddress email, CredentialHash credentialHash, String displayName,
            AccountStatus status, VerificationStatus verificationStatus, Instant verifiedAt,
            Instant lastLoginAt, int failedLoginCount, long version, Set<RoleCode> roles, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
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
        return new Account(id, email, credentialHash, displayName, AccountStatus.ACTIVE,
            VerificationStatus.UNVERIFIED, null, null, 0, 0L, EnumSet.of(RoleCode.CUSTOMER), now);
    }

    /** Reconstitutes a persisted account. No event is raised — rehydration is not a business event. */
    public static Account reconstitute(UUID id, EmailAddress email, CredentialHash credentialHash,
            String displayName, AccountStatus status, VerificationStatus verificationStatus,
            Instant verifiedAt, Instant lastLoginAt, int failedLoginCount, long version, Set<RoleCode> roles,
            Instant createdAt) {
        return new Account(id, email, credentialHash, displayName, status, verificationStatus, verifiedAt,
            lastLoginAt, failedLoginCount, version, roles, createdAt);
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
