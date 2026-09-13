package org.phuchoang.ecp.identity.api;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.IdentityApplicationService;
import org.phuchoang.ecp.identity.application.command.AddressCommand;
import org.phuchoang.ecp.identity.application.command.ChangePasswordCommand;
import org.phuchoang.ecp.identity.application.command.LoginCommand;
import org.phuchoang.ecp.identity.application.command.RegisterAccountCommand;
import org.phuchoang.ecp.identity.application.command.UpdateProfileCommand;
import org.phuchoang.ecp.identity.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.application.mapper.AddressSummary;
import org.phuchoang.ecp.identity.application.mapper.LoginResult;
import org.phuchoang.ecp.identity.application.query.AddressPageResult;
import org.phuchoang.ecp.sharedkernel.api.Address;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

    public SessionResponse renewSession(RenewSessionRequest request) {
        LoginResult result = applicationService.renewSession(request.refreshToken());
        return new SessionResponse(result.accessToken(), result.refreshToken(), result.expiresInSeconds(),
            result.restricted(), toAccountView(result.account()));
    }

    public void changeOwnPassword(Actor actor, ChangePasswordRequest request) {
        boolean endOtherSessions = request.endOtherSessions() == null || request.endOtherSessions();
        applicationService.changeOwnPassword(toCallerContext(actor),
            new ChangePasswordCommand(request.currentPassword(), request.newPassword(), endOtherSessions));
    }

    public void requestPasswordReset(PasswordResetRequestRequest request) {
        applicationService.requestPasswordReset(request.email());
    }

    public void completePasswordReset(PasswordResetCompletionRequest request) {
        applicationService.completePasswordReset(request.token(), request.newPassword());
    }

    public AccountView getOwnAccount(Actor actor) {
        return toAccountView(applicationService.getOwnAccount(toCallerContext(actor)));
    }

    public AccountView updateOwnProfile(Actor actor, ProfileUpdateRequest request) {
        AccountSummary result = applicationService.updateOwnProfile(toCallerContext(actor),
            new UpdateProfileCommand(request.displayName(), request.email()));
        return toAccountView(result);
    }

    public AddressPageView listOwnAddresses(Actor actor, String cursor, int size) {
        AddressPageResult result = applicationService.listOwnAddresses(toCallerContext(actor), cursor, size);
        return new AddressPageView(result.items().stream().map(IdentityFacade::toAddressView).toList(),
            result.nextCursor());
    }

    public AddressView addOwnAddress(Actor actor, AddressWriteRequest request) {
        return toAddressView(applicationService.addOwnAddress(toCallerContext(actor), toAddressCommand(request)));
    }

    public AddressView getOwnAddress(Actor actor, UUID addressId) {
        return toAddressView(applicationService.getOwnAddress(toCallerContext(actor), addressId));
    }

    public AddressView replaceOwnAddress(Actor actor, UUID addressId, AddressWriteRequest request) {
        return toAddressView(applicationService.replaceOwnAddress(toCallerContext(actor), addressId,
            toAddressCommand(request)));
    }

    public void removeOwnAddress(Actor actor, UUID addressId) {
        applicationService.removeOwnAddress(toCallerContext(actor), addressId);
    }

    private static AddressCommand toAddressCommand(AddressWriteRequest request) {
        Address address = new Address(request.label(), request.recipientName(), request.line1(), request.line2(),
            request.city(), request.region(), request.postalCode(), request.countryCode(), request.phone());
        boolean defaultShipping = request.isDefaultShipping() != null && request.isDefaultShipping();
        boolean defaultBilling = request.isDefaultBilling() != null && request.isDefaultBilling();
        return new AddressCommand(address, defaultShipping, defaultBilling);
    }

    private static AddressView toAddressView(AddressSummary summary) {
        return new AddressView(summary.id(), summary.label(), summary.recipientName(), summary.line1(),
            summary.line2(), summary.city(), summary.region(), summary.postalCode(), summary.countryCode(),
            summary.phone(), summary.isDefaultShipping(), summary.isDefaultBilling());
    }

    private static AccountView toAccountView(AccountSummary summary) {
        return new AccountView(summary.id(), summary.email(), summary.displayName(), summary.status(),
            summary.verificationStatus(), summary.pendingEmail(), summary.roles(), summary.verifiedAt(),
            summary.lastLoginAt(), summary.createdAt());
    }

    private static CallerContext toCallerContext(Actor actor) {
        return CallerContext.of(actor.accountId(), actor.roles());
    }
}
