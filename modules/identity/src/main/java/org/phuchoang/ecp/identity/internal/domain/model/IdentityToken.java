package org.phuchoang.ecp.identity.internal.domain.model;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A single-use, expiring token — {@code identity_token} (`BR-CUS-03`). Covers verification,
 * password-reset, and refresh tokens: the schema unifies all three under one table with a
 * {@code token_type} discriminator, and this domain type mirrors that.
 *
 * <p>Consumption itself is a database-arbitrated conditional update
 * (`UPDATE ... WHERE consumed_at IS NULL`, Sequence/04-Identity.md §4) so two simultaneous
 * presentations of one token cannot both succeed — the same reasoning as `BR-CUS-01`'s unique
 * index. This object's {@link #isExpired(Clock)}/{@link #isConsumed()} are for deciding which
 * response to give, not the concurrency guarantee itself.
 *
 * <p>{@code chainId} (Sprint 04, `US-CUS-05`, ADR-0016 §4) links every token produced by rotating
 * one login's refresh token together — a fresh login's refresh token is its own chain's head
 * ({@code chainId == id}); every rotation inherits the presented token's {@code chainId}. Only
 * {@link TokenType#REFRESH} tokens carry one; verification/reset tokens never rotate.
 */
@Entity
public final class IdentityToken {

    @Identity
    private final UUID id;
    private final UUID accountId;
    private final TokenType type;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Instant consumedAt;
    private final UUID replacedBy;
    private final UUID chainId;

    public IdentityToken(UUID id, UUID accountId, TokenType type, String tokenHash, Instant issuedAt,
            Instant expiresAt, Instant consumedAt, UUID replacedBy, UUID chainId) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.type = Objects.requireNonNull(type);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.consumedAt = consumedAt;
        this.replacedBy = replacedBy;
        this.chainId = chainId;
    }

    public static IdentityToken issue(UUID id, UUID accountId, TokenType type, String tokenHash,
            java.time.Duration ttl, Clock clock) {
        Instant now = Instant.now(clock);
        UUID chainId = type == TokenType.REFRESH ? id : null;
        return new IdentityToken(id, accountId, type, tokenHash, now, now.plus(ttl), null, null, chainId);
    }

    /** A rotated refresh token: inherits {@code chainId} from the token it replaces. */
    public static IdentityToken rotate(UUID id, UUID accountId, String tokenHash, java.time.Duration ttl,
            UUID chainId, Clock clock) {
        Instant now = Instant.now(clock);
        return new IdentityToken(id, accountId, TokenType.REFRESH, tokenHash, now, now.plus(ttl), null, null,
            Objects.requireNonNull(chainId));
    }

    public boolean isExpired(Clock clock) {
        return !Instant.now(clock).isBefore(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isUsable(Clock clock) {
        return !isConsumed() && !isExpired(clock);
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public TokenType type() {
        return type;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    public UUID replacedBy() {
        return replacedBy;
    }

    public UUID chainId() {
        return chainId;
    }
}
