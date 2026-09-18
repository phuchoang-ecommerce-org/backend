package org.phuchoang.ecp.identity.internal.application.password;

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

/**
 * The password-reset token protocol (`UC-CUS-07`): at most one live token per account (A2),
 * one-hour lifetime, single use. Lookup and consumption are deliberately two steps so the use
 * case can validate the new password <em>before</em> consuming the token (E3: a policy failure
 * must leave the link usable for a retry).
 */
@Component
public class PasswordResetTokenManager {

    static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final TokenStore tokens;
    private final Clock clock;

    public PasswordResetTokenManager(TokenStore tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Invalidates any outstanding reset token first, then issues exactly one. @return the raw token for the link. */
    public String issue(UUID accountId) {
        tokens.invalidateOutstanding(accountId, TokenType.PASSWORD_RESET);
        String rawToken = OpaqueTokens.generate();
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.PASSWORD_RESET,
            OpaqueTokens.hash(rawToken), RESET_TOKEN_TTL, clock);
        tokens.save(token);
        return rawToken;
    }

    /** The reset {@code rawToken} names, if it is currently usable; nothing is consumed yet. */
    public Optional<PendingReset> findUsable(String rawToken) {
        return tokens.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.PASSWORD_RESET)
            .filter(token -> token.isUsable(clock))
            .map(token -> new PendingReset(token.id(), token.accountId()));
    }

    /** @return {@code false} when a concurrent completion consumed the token first. */
    public boolean consume(PendingReset reset) {
        return tokens.consumeIfUsable(reset.tokenId(), Instant.now(clock));
    }

    public record PendingReset(UUID tokenId, UUID accountId) {
    }
}
