package org.phuchoang.ecp.identity.internal.application.port;

import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal persistence gateway for Account-owned tokens. It is not a DDD repository; the rotation/
 * single-use guarantee: a conditional {@code UPDATE ... WHERE consumed_at IS NULL AND
 * expires_at > now()}, so two simultaneous presentations of the same token cannot both succeed
 * (`BR-CUS-03`, Sequence/04-Identity.md §4/§7) — this is checked at the database, never by a
 * preceding read-then-write.
 */
public interface TokenStore {

    IdentityToken save(IdentityToken token);

    Optional<IdentityToken> findByTokenHash(String tokenHash, TokenType type);

    /** @return {@code true} if exactly one row was consumed; {@code false} if it was already consumed, expired, or unknown. */
    boolean consumeIfUsable(UUID tokenId, Instant now);

    /**
     * Atomically consumes {@code tokenId} and links it to the token that replaced it, in one
     * conditional update (`US-CUS-05`, ADR-0016 §4) — the rotation counterpart of
     * {@link #consumeIfUsable}, which does not record {@code replaced_by}.
     *
     * @return {@code true} if exactly one row was consumed-and-linked; {@code false} if it was
     *     already consumed, expired, or unknown — the caller must treat {@code false} as a
     *     reuse/race and invalidate the whole chain via {@link #invalidateChain}.
     */
    boolean rotateIfUsable(UUID tokenId, UUID replacedByTokenId, Instant now);

    /** Invalidates the outstanding (unconsumed) tokens of {@code type} for one account — e.g. a resend, or "log out everywhere". */
    void invalidateOutstanding(UUID accountId, TokenType type);

    /**
     * Invalidates every outstanding (unconsumed) token sharing {@code chainId} — the reuse-detection
     * response (`US-CUS-05`, ADR-0016 §4): presenting an already-rotated refresh token again
     * invalidates the whole chain, not just that one token.
     */
    void invalidateChain(UUID chainId, Instant now);
}
