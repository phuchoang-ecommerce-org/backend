package org.phuchoang.ecp.identity.internal.infrastructure.persistence.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;

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

    @Column(name = "chain_id")
    private UUID chainId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TokenEntity() {
        // JPA
    }

    TokenEntity(UUID id, UUID accountId, TokenType tokenType, String tokenHash, Instant issuedAt,
            Instant expiresAt, Instant consumedAt, UUID replacedBy, UUID chainId) {
        this.id = id;
        this.accountId = accountId;
        this.tokenType = tokenType;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.replacedBy = replacedBy;
        this.chainId = chainId;
        this.createdAt = issuedAt;
    }

    UUID getId() {
        return id;
    }
    public void setId(UUID id) { this.id = id; }

    UUID getAccountId() {
        return accountId;
    }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    TokenType getTokenType() {
        return tokenType;
    }
    public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

    String getTokenHash() {
        return tokenHash;
    }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    Instant getIssuedAt() {
        return issuedAt;
    }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    Instant getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    Instant getConsumedAt() {
        return consumedAt;
    }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

    UUID getReplacedBy() {
        return replacedBy;
    }
    public void setReplacedBy(UUID replacedBy) { this.replacedBy = replacedBy; }

    UUID getChainId() {
        return chainId;
    }
    public void setChainId(UUID chainId) { this.chainId = chainId; }

    void initializeCreatedAt(Instant now) { this.createdAt = now; }
}
