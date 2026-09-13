package org.phuchoang.ecp.identity.application;

import org.phuchoang.ecp.identity.application.command.AddressCommand;
import org.phuchoang.ecp.identity.application.command.AddressService;
import org.phuchoang.ecp.identity.application.command.ChangePasswordCommand;
import org.phuchoang.ecp.identity.application.command.ChangePasswordService;
import org.phuchoang.ecp.identity.application.command.LoginCommand;
import org.phuchoang.ecp.identity.application.command.LoginService;
import org.phuchoang.ecp.identity.application.command.LogoutService;
import org.phuchoang.ecp.identity.application.command.PasswordResetService;
import org.phuchoang.ecp.identity.application.command.ProfileService;
import org.phuchoang.ecp.identity.application.command.RegisterAccountCommand;
import org.phuchoang.ecp.identity.application.command.RegisterAccountService;
import org.phuchoang.ecp.identity.application.command.RenewSessionService;
import org.phuchoang.ecp.identity.application.command.UpdateProfileCommand;
import org.phuchoang.ecp.identity.application.command.VerifyEmailService;
import org.phuchoang.ecp.identity.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.application.mapper.AddressSummary;
import org.phuchoang.ecp.identity.application.mapper.LoginResult;
import org.phuchoang.ecp.identity.application.query.AddressPageResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
    private final RenewSessionService renewSessionService;
    private final ChangePasswordService changePasswordService;
    private final PasswordResetService passwordResetService;
    private final ProfileService profileService;
    private final AddressService addressService;

    IdentityApplicationService(RegisterAccountService registerAccountService, VerifyEmailService verifyEmailService,
            LoginService loginService, LogoutService logoutService, RenewSessionService renewSessionService,
            ChangePasswordService changePasswordService, PasswordResetService passwordResetService,
            ProfileService profileService, AddressService addressService) {
        this.registerAccountService = registerAccountService;
        this.verifyEmailService = verifyEmailService;
        this.loginService = loginService;
        this.logoutService = logoutService;
        this.renewSessionService = renewSessionService;
        this.changePasswordService = changePasswordService;
        this.passwordResetService = passwordResetService;
        this.profileService = profileService;
        this.addressService = addressService;
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

    public LoginResult renewSession(String refreshToken) {
        return renewSessionService.renewSession(refreshToken);
    }

    public void changeOwnPassword(CallerContext caller, ChangePasswordCommand command) {
        changePasswordService.changeOwnPassword(caller, command);
    }

    public void requestPasswordReset(String email) {
        passwordResetService.requestPasswordReset(email);
    }

    public void completePasswordReset(String token, String newPassword) {
        passwordResetService.completePasswordReset(token, newPassword);
    }

    public AccountSummary getOwnAccount(CallerContext caller) {
        return profileService.getOwnAccount(caller);
    }

    public AccountSummary updateOwnProfile(CallerContext caller, UpdateProfileCommand command) {
        return profileService.updateOwnProfile(caller, command);
    }

    public AddressPageResult listOwnAddresses(CallerContext caller, String cursor, int size) {
        return addressService.listOwnAddresses(caller, cursor, size);
    }

    public AddressSummary addOwnAddress(CallerContext caller, AddressCommand command) {
        return addressService.addOwnAddress(caller, command);
    }

    public AddressSummary getOwnAddress(CallerContext caller, UUID addressId) {
        return addressService.getOwnAddress(caller, addressId);
    }

    public AddressSummary replaceOwnAddress(CallerContext caller, UUID addressId, AddressCommand command) {
        return addressService.replaceOwnAddress(caller, addressId, command);
    }

    public void removeOwnAddress(CallerContext caller, UUID addressId) {
        addressService.removeOwnAddress(caller, addressId);
    }
}
