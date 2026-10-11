package org.phuchoang.ecp.identity.internal.infrastructure.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.phuchoang.ecp.identity.internal.domain.model.AccountStatus;
import org.phuchoang.ecp.identity.internal.domain.model.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping of {@code identity_account} (Database.md §4.1) — the persistence shape only; {@link org.phuchoang.ecp.identity.internal.domain.model.Account} is the behaviour. */
@Entity
@Table(name = "identity_account")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "credential_hash", nullable = false)
    private String credentialHash;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "pending_email")
    private String pendingEmail;

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
    @Setter(AccessLevel.NONE)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Column(name = "updated_by")
    @Setter(AccessLevel.NONE)
    private UUID updatedBy;

    AccountEntity(UUID id, String email, String pendingEmail, String credentialHash, String displayName,
            AccountStatus status, VerificationStatus verificationStatus, Instant verifiedAt, Instant lastLoginAt,
            int failedLoginCount, long version, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.pendingEmail = pendingEmail;
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

    void initializeAuditTimestamps(Instant now) { this.createdAt = now; this.updatedAt = now; }
}
