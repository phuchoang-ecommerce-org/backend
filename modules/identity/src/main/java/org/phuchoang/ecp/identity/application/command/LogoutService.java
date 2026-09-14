package org.phuchoang.ecp.identity.application.command;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.SessionEnded;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * `UC-CUS-04` — Log Out (`US-CUS-04`): {@code logOut} and {@code endAllOwnSessions}. E1
 * (already ended / unrecognised token) is success-with-no-action, never an error — the caller's
 * goal (not being logged in) already holds, and an error here only invites a useless retry.
 *
 * <p>This backend carries no cookie ({@code ADR-0025} is a Next.js-server concern outside this
 * repo — see Sprint 03 plan). "Both halves" of a real logout here means: consume the specific
 * refresh token server-side. Because {@code DELETE /sessions/current} otherwise has no way to
 * name which refresh token to revoke, this sprint adds a small request body carrying it — an
 * intentional, logged drift from the original contract, reconciled in {@code openapi.yaml} the
 * same session (Gate G1 check 8; see Sprint 03 Review Notes).
 */
@Service
public class LogoutService {

    private final TokenRepository tokenRepository;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LogoutService(TokenRepository tokenRepository, AuthorizationService authorizationService,
            ApplicationEventPublisher events, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.authorizationService = authorizationService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void logOut(CallerContext caller, String rawRefreshToken) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.LOG_OUT);

        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            Optional<IdentityToken> token =
                tokenRepository.findByTokenHash(RefreshTokenGenerator.hash(rawRefreshToken), TokenType.REFRESH);
            // US-CUS-05: invalidate the whole chain, not just the presented token — a token
            // rotated moments before this logOut call must not survive it in the same chain.
            token.ifPresent(t -> tokenRepository.invalidateChain(t.chainId(), Instant.now(clock)));
            // Absent, expired, or already consumed — E1: success, no action.
        }
        events.publishEvent(new SessionEnded(caller.accountId(), false, Instant.now(clock)));
    }

    @Transactional
    public void endAllOwnSessions(CallerContext caller) {
        authorizationService.assertAuthorized(caller, PermissionMatrix.END_ALL_OWN_SESSIONS);
        tokenRepository.invalidateOutstanding(caller.accountId(), TokenType.REFRESH);
        events.publishEvent(new SessionEnded(caller.accountId(), true, Instant.now(clock)));
    }
}
