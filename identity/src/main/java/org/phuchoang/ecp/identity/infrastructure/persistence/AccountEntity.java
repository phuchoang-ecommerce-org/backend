package org.phuchoang.ecp.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.phuchoang.ecp.identity.domain.AccountStatus;
import org.phuchoang.ecp.identity.domain.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping of {@code identity_account} (Database.md §4.1) — the persistence shape only; {@link org.phuchoang.ecp.identity.domain.Account} is the behaviour. */
@Entity
@Table(name = "identity_account")
class AccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "credential_hash", nullable = false)
    private String credentialHash;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 16)
    private VerificationStatus verificationStatus;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected AccountEntity() {
        // JPA
    }

    AccountEntity(UUID id, String email, String credentialHash, String displayName, AccountStatus status,
            VerificationStatus verificationStatus, Instant verifiedAt, Instant lastLoginAt, int failedLoginCount,
            long version, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.credentialHash = credentialHash;
        this.displayName = displayName;
        this.status = status;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
        this.lastLoginAt = lastLoginAt;
        this.failedLoginCount = failedLoginCount;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getCredentialHash() {
        return credentialHash;
    }

    String getDisplayName() {
        return displayName;
    }

    AccountStatus getStatus() {
        return status;
    }

    VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    Instant getVerifiedAt() {
        return verifiedAt;
    }

    void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    Instant getLastLoginAt() {
        return lastLoginAt;
    }

    void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    int getFailedLoginCount() {
        return failedLoginCount;
    }

    void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }

    long getVersion() {
        return version;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
