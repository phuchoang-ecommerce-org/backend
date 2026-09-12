package org.phuchoang.ecp.identity.application.internal;

import org.phuchoang.ecp.identity.application.command.RegisterAccountService;
import org.phuchoang.ecp.identity.application.command.VerifyEmailService;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.TokenType;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

/**
 * Shared by {@link RegisterAccountService} (initial issue) and {@link VerifyEmailService}
 * (resend) — both invalidate any outstanding token first (`UC-CUS-02` A2: at most one live at a
 * time) and issue exactly one new one.
 */
public final class VerificationTokenIssuing {

    static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);

    private VerificationTokenIssuing() {
    }

    /** @return the raw (unhashed) token to include in the verification link/email. */
    public static String issue(TokenRepository tokenRepository, UUID accountId, Clock clock) {
        tokenRepository.invalidateOutstanding(accountId, TokenType.EMAIL_VERIFICATION);
        String rawToken = RefreshTokenGenerator.generate();
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), accountId, TokenType.EMAIL_VERIFICATION,
            RefreshTokenGenerator.hash(rawToken), EMAIL_VERIFICATION_TTL, clock);
        tokenRepository.save(token);
        return rawToken;
    }
}
