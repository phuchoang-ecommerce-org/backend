package org.phuchoang.ecp.identity.internal.application.command;

import org.phuchoang.ecp.identity.internal.application.CallerContext;
import org.phuchoang.ecp.identity.internal.application.command.model.LoginCommand;
import org.phuchoang.ecp.identity.internal.application.internal.PermissionMatrix;
import org.phuchoang.ecp.identity.internal.application.internal.RefreshTokenGenerator;
import org.phuchoang.ecp.identity.internal.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.internal.application.mapper.LoginResult;
import org.phuchoang.ecp.identity.internal.application.port.AccessTokenIssuer;
import org.phuchoang.ecp.identity.internal.domain.repository.AccountRepository;
import org.phuchoang.ecp.identity.internal.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.internal.application.port.PasswordEncoder;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.domain.model.Account;
import org.phuchoang.ecp.identity.internal.domain.model.AccountStatus;
import org.phuchoang.ecp.identity.internal.domain.model.EmailAddress;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.event.SessionEstablished;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * `UC-CUS-03` — Log In (`US-CUS-03`). E1 (bad credentials) and E3 (suspended account) return the
 * exact same {@code ECP-GEN-4010} with the exact same message — `BR-CUS-04` requires the caller
 * cannot distinguish "no such account", "wrong password", and "suspended" from the response.
 *
 * <p>Cart merge (`UC-CRT-05`) is out of scope this sprint — the `cart` module does not exist yet.
 * {@code renewSession} (`UC-CUS-05`) is likewise out of scope; this service issues the initial
 * refresh token (rotation-capable) but nothing exposes a renewal endpoint yet.
 */
@Service
public class LoginService {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final String GENERIC_FAILURE = "Email or password is incorrect.";

    private final AccountRepository accountRepository;
    private final TokenStore tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoginService(AccountRepository accountRepository, TokenStore tokenRepository,
            PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokenIssuer,
            AuthorizationService authorizationService, ApplicationEventPublisher events, Clock clock) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.authorizationService = authorizationService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public LoginResult logIn(LoginCommand command) {
        authorizationService.assertAuthorized(CallerContext.GUEST, PermissionMatrix.LOG_IN);

        Account account = accountRepository.findByEmail(new EmailAddress(command.email()))
            .orElseThrow(this::genericFailure); // E1 — unknown address, same message as wrong password.

        if (!passwordEncoder.matches(command.password(), account.credentialHash().value())) {
            account.recordFailedLogin();
            accountRepository.save(account);
            throw genericFailure(); // E1.
        }
        if (account.status() != AccountStatus.ACTIVE) {
            throw genericFailure(); // E3 — suspended is reported exactly like a credential failure.
        }

        account.recordSuccessfulLogin(clock);
        accountRepository.save(account);

        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(account.id(), account.roles());

        String rawRefreshToken = RefreshTokenGenerator.generate();
        IdentityToken refreshToken = IdentityToken.issue(UUID.randomUUID(), account.id(), TokenType.REFRESH,
            RefreshTokenGenerator.hash(rawRefreshToken), REFRESH_TOKEN_TTL, clock);
        tokenRepository.save(refreshToken);

        boolean restricted = !account.isVerified(); // A1 — restricted session, browse/cart only.
        events.publishEvent(new SessionEstablished(account.id(), restricted, Instant.now(clock)));

        return new LoginResult(accessToken.token(), rawRefreshToken, accessToken.expiresInSeconds(), restricted,
            AccountSummary.of(account));
    }

    private DomainException genericFailure() {
        return new DomainException(GenErrorCode.NOT_AUTHENTICATED, GENERIC_FAILURE);
    }
}
