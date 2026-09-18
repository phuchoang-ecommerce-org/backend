package org.phuchoang.ecp.identity.internal.application.registration;

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
 * The email-verification token protocol (`UC-CUS-02`): at most one live token per account (A2),
 * 24-hour lifetime, single use. Issued by registration, resend, and an email-change request
 * (`UC-CUS-08` A1); consumed exactly once, atomically at the database, by
 * {@link RegistrationUseCases#verifyEmailAddress}.
 */
@Component
public class VerificationTokenManager {

    static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);

    private final TokenStore tokens;
    private final Clock clock;

    public VerificationTokenManager(TokenStore tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    /** Invalidates any outstanding token first, then issues exactly one. @return the raw token for the link. */
    public String issue(UUID accountId) {
        tokens.invalidateOutstanding(accountId, TokenType.EMAIL_VERIFICATION);
        String rawToken = OpaqueTokens.generate();
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.EMAIL_VERIFICATION,
            OpaqueTokens.hash(rawToken), EMAIL_VERIFICATION_TTL, clock);
        tokens.save(token);
        return rawToken;
    }

    /**
     * Consumes {@code rawToken} if it is still usable. Empty for an unrecognised, expired,
     * already-consumed, or concurrently-consumed token — the caller reports all of these
     * identically.
     *
     * @return the account the token was issued for
     */
    public Optional<UUID> consume(String rawToken) {
        return tokens.findByTokenHash(OpaqueTokens.hash(rawToken), TokenType.EMAIL_VERIFICATION)
            .filter(token -> token.isUsable(clock))
            .filter(token -> tokens.consumeIfUsable(token.id(), Instant.now(clock)))
            .map(IdentityToken::accountId);
    }
}
