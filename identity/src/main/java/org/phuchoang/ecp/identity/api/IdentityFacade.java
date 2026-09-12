package org.phuchoang.ecp.identity.api;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.IdentityApplicationService;
import org.phuchoang.ecp.identity.application.command.LoginCommand;
import org.phuchoang.ecp.identity.application.command.RegisterAccountCommand;
import org.phuchoang.ecp.identity.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.application.mapper.LoginResult;
import org.springframework.stereotype.Component;

/**
 * The reachable surface of the {@code identity} module (`@NamedInterface`). {@code app}'s web
 * layer calls only this — never {@code identity.application} directly
 * (`noClassReachesIntoAnotherModulesApplicationPackage`). Thin by design: every method translates
 * between wire-shaped api records and the application layer's commands/results, and nothing else.
 */
@Component
public final class IdentityFacade {

    private final IdentityApplicationService applicationService;

    IdentityFacade(IdentityApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void registerAccount(RegisterAccountRequest request) {
        applicationService.registerAccount(
            new RegisterAccountCommand(request.email(), request.password(), request.displayName()));
    }

    public void verifyEmailAddress(String token) {
        applicationService.verifyEmailAddress(token);
    }

    public void resendEmailVerification(String email) {
        applicationService.resendEmailVerification(email);
    }

    public SessionResponse logIn(LoginRequest request) {
        LoginResult result = applicationService.logIn(new LoginCommand(request.email(), request.password()));
        return new SessionResponse(result.accessToken(), result.refreshToken(), result.expiresInSeconds(),
            result.restricted(), toAccountView(result.account()));
    }

    public void logOut(Actor actor, String refreshToken) {
        applicationService.logOut(toCallerContext(actor), refreshToken);
    }

    public void endAllOwnSessions(Actor actor) {
        applicationService.endAllOwnSessions(toCallerContext(actor));
    }

    private static AccountView toAccountView(AccountSummary summary) {
        return new AccountView(summary.id(), summary.email(), summary.status(), summary.verificationStatus(),
            summary.roles(), summary.verifiedAt(), summary.lastLoginAt(), summary.createdAt());
    }

    private static CallerContext toCallerContext(Actor actor) {
        return CallerContext.of(actor.accountId(), actor.roles());
    }
}
