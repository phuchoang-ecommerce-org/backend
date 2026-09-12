package org.phuchoang.ecp.identity.application.port;

import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.TokenType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The persistence port for {@link IdentityToken}. {@link #consumeIfUsable} is the rotation/
 * single-use guarantee: a conditional {@code UPDATE ... WHERE consumed_at IS NULL AND
 * expires_at > now()}, so two simultaneous presentations of the same token cannot both succeed
 * (`BR-CUS-03`, Sequence/04-Identity.md §4/§7) — this is checked at the database, never by a
 * preceding read-then-write.
 */
public interface TokenRepository {

    IdentityToken save(IdentityToken token);

    Optional<IdentityToken> findByTokenHash(String tokenHash, TokenType type);

    /** @return {@code true} if exactly one row was consumed; {@code false} if it was already consumed, expired, or unknown. */
    boolean consumeIfUsable(UUID tokenId, Instant now);

    /** Invalidates the outstanding (unconsumed) tokens of {@code type} for one account — e.g. a resend, or "log out everywhere". */
    void invalidateOutstanding(UUID accountId, TokenType type);
}
