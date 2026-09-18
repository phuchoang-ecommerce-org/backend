package org.phuchoang.ecp.identity.internal.application.authentication;

import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.application.token.OpaqueTokens;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** {@link SessionManager} over {@code identity_token} rows of type {@code REFRESH}. */
@Component
public class RefreshTokenSessionManager implements SessionManager {

    static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    private final TokenStore tokens;
    private final Clock clock;

    public RefreshTokenSessionManager(TokenStore tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public String issue(UUID accountId) {
        String rawRefreshToken = OpaqueTokens.generate();
        IdentityToken refreshToken = IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.REFRESH,
            OpaqueTokens.hash(rawRefreshToken), REFRESH_TOKEN_TTL, clock);
        tokens.save(refreshToken);
        return rawRefreshToken;
    }

    @Override
    public Presentation present(String rawRefreshToken) {
        Optional<IdentityToken> found = tokens.findByTokenHash(OpaqueTokens.hash(rawRefreshToken), TokenType.REFRESH);
        if (found.isEmpty()) {
            return new Presentation.Invalid();
        }
        IdentityToken presented = found.get();
        if (presented.isConsumed()) {
            // Reuse: the token has already been rotated away (or raced away). The caller cannot
            // tell an attacker-replay from a losing race, and doesn't need to: either way, the
            // whole chain is no longer trustworthy.
            tokens.invalidateChain(presented.chainId(), Instant.now(clock));
            return new Presentation.Reused(presented.accountId());
        }
        if (presented.isExpired(clock)) {
            // Ordinary expiry, not reuse — the chain is still fine, the caller just needs to log in again.
            return new Presentation.Invalid();
        }
        return new Presentation.Usable(presented.accountId(), presented.id(), presented.chainId());
    }

    @Override
    public Optional<String> rotate(Presentation.Usable presented) {
        // The new row must exist before the old row's replaced_by can point at it —
        // fk_identity_token_replaced_by is not deferrable (Database.md §4.1).
        UUID newTokenId = UUID.randomUUID();
        String rawNewRefreshToken = OpaqueTokens.generate();
        IdentityToken newRefreshToken = IdentityToken.rotate(newTokenId, presented.accountId(),
            OpaqueTokens.hash(rawNewRefreshToken), REFRESH_TOKEN_TTL, presented.chainId(), clock);
        tokens.save(newRefreshToken);

        boolean rotated = tokens.rotateIfUsable(presented.tokenId(), newTokenId, Instant.now(clock));
        if (!rotated) {
            // Lost a race against a concurrent refresh of the same token — treat identically to
            // reuse. The new row saved above becomes an orphaned, never-issued token — harmless
            // (it was never handed to any caller) and consumed along with the rest of the chain.
            tokens.invalidateChain(presented.chainId(), Instant.now(clock));
            return Optional.empty();
        }
        return Optional.of(rawNewRefreshToken);
    }

    @Override
    public void revoke(String rawRefreshToken) {
        // US-CUS-05: invalidate the whole chain, not just the presented token — a token rotated
        // moments before this call must not survive it in the same chain.
        tokens.findByTokenHash(OpaqueTokens.hash(rawRefreshToken), TokenType.REFRESH)
            .ifPresent(token -> tokens.invalidateChain(token.chainId(), Instant.now(clock)));
    }

    @Override
    public void revokeAll(UUID accountId) {
        tokens.invalidateOutstanding(accountId, TokenType.REFRESH);
    }
}
