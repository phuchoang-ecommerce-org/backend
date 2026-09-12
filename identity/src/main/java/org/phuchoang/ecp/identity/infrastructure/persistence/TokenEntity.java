package org.phuchoang.ecp.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.phuchoang.ecp.identity.domain.TokenType;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping of {@code identity_token} (Database.md §4.1). */
@Entity
@Table(name = "identity_token")
class TokenEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 24)
    private TokenType tokenType;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TokenEntity() {
        // JPA
    }

    TokenEntity(UUID id, UUID accountId, TokenType tokenType, String tokenHash, Instant issuedAt,
            Instant expiresAt, Instant consumedAt, UUID replacedBy) {
        this.id = id;
        this.accountId = accountId;
        this.tokenType = tokenType;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.replacedBy = replacedBy;
        this.createdAt = issuedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getAccountId() {
        return accountId;
    }

    TokenType getTokenType() {
        return tokenType;
    }

    String getTokenHash() {
        return tokenHash;
    }

    Instant getIssuedAt() {
        return issuedAt;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getConsumedAt() {
        return consumedAt;
    }

    UUID getReplacedBy() {
        return replacedBy;
    }
}
