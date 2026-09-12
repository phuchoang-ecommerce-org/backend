package org.phuchoang.ecp.identity.application;

import org.phuchoang.ecp.identity.application.command.LoginCommand;
import org.phuchoang.ecp.identity.application.command.LoginService;
import org.phuchoang.ecp.identity.application.command.LogoutService;
import org.phuchoang.ecp.identity.application.command.RegisterAccountCommand;
import org.phuchoang.ecp.identity.application.command.RegisterAccountService;
import org.phuchoang.ecp.identity.application.command.VerifyEmailService;
import org.phuchoang.ecp.identity.application.mapper.LoginResult;
import org.springframework.stereotype.Service;

/**
 * The single public entry point into {@code identity.application} — everything else in this
 * subtree is public only so its own {@code command}/{@code query}/{@code mapper}/{@code internal}
 * packages can see each other; the module boundary is enforced at the whole-package level, by
 * {@code identity.api.IdentityFacade} being the only external caller
 * (`noClassReachesIntoAnotherModulesApplicationPackage`), not by Java visibility inside it.
 */
@Service
public class IdentityApplicationService {

    private final RegisterAccountService registerAccountService;
    private final VerifyEmailService verifyEmailService;
    private final LoginService loginService;
    private final LogoutService logoutService;

    IdentityApplicationService(RegisterAccountService registerAccountService, VerifyEmailService verifyEmailService,
            LoginService loginService, LogoutService logoutService) {
        this.registerAccountService = registerAccountService;
        this.verifyEmailService = verifyEmailService;
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    public void registerAccount(RegisterAccountCommand command) {
        registerAccountService.registerAccount(command);
    }

    public void verifyEmailAddress(String token) {
        verifyEmailService.verifyEmailAddress(token);
    }

    public void resendEmailVerification(String email) {
        verifyEmailService.resendEmailVerification(email);
    }

    public LoginResult logIn(LoginCommand command) {
        return loginService.logIn(command);
    }

    public void logOut(CallerContext caller, String refreshToken) {
        logoutService.logOut(caller, refreshToken);
    }

    public void endAllOwnSessions(CallerContext caller) {
        logoutService.endAllOwnSessions(caller);
    }
}
