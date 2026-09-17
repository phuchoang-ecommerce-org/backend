package org.phuchoang.ecp.identity.api.facade;

import org.phuchoang.ecp.identity.api.authorization.Actor;
import org.phuchoang.ecp.identity.api.request.AddressWriteRequest;
import org.phuchoang.ecp.identity.api.request.ChangePasswordRequest;
import org.phuchoang.ecp.identity.api.request.LoginRequest;
import org.phuchoang.ecp.identity.api.request.PasswordResetCompletionRequest;
import org.phuchoang.ecp.identity.api.request.PasswordResetRequestRequest;
import org.phuchoang.ecp.identity.api.request.ProfileUpdateRequest;
import org.phuchoang.ecp.identity.api.request.RegisterAccountRequest;
import org.phuchoang.ecp.identity.api.request.RenewSessionRequest;
import org.phuchoang.ecp.identity.api.view.AccountView;
import org.phuchoang.ecp.identity.api.view.AddressPageView;
import org.phuchoang.ecp.identity.api.view.AddressView;
import org.phuchoang.ecp.identity.api.view.SessionResponse;
import org.phuchoang.ecp.identity.internal.application.CallerContext;
import org.phuchoang.ecp.identity.internal.application.IdentityApplicationService;
import org.phuchoang.ecp.identity.internal.application.command.model.ChangePasswordCommand;
import org.phuchoang.ecp.identity.internal.application.mapper.AccountSummary;
import org.phuchoang.ecp.identity.internal.application.mapper.AddressSummary;
import org.phuchoang.ecp.identity.internal.application.mapper.LoginResult;
import org.phuchoang.ecp.identity.internal.application.query.AddressPageResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The reachable surface of the {@code identity} module (`@NamedInterface`). {@code app}'s web
 * layer calls only this — never {@code identity.internal.application} directly
 * (`noClassReachesIntoAnotherModulesApplicationPackage`). Thin by design: every method translates
 * between wire-shaped api records and the application layer's commands/results, and nothing else.
 */
@Component
public final class IdentityFacade {

    private final IdentityApplicationService applicationService;
    private final IdentityDtoMapper mapper;

    IdentityFacade(IdentityApplicationService applicationService, IdentityDtoMapper mapper) {
        this.applicationService = applicationService;
        this.mapper = mapper;
    }

    /** Registers an account and starts its email-verification flow ({@code UC-CUS-01}). */
    public void registerAccount(RegisterAccountRequest request) {
        applicationService.registerAccount(mapper.registerAccountCommand(request));
    }

    /** Proves ownership of a pending email address using its single-use verification token. */
    public void verifyEmailAddress(String token) {
        applicationService.verifyEmailAddress(token);
    }

    /** Invalidates a prior verification token and requests delivery of a replacement. */
    public void resendEmailVerification(String email) {
        applicationService.resendEmailVerification(email);
    }

    /** Authenticates credentials and returns a new access/refresh session pair. */
    public SessionResponse logIn(LoginRequest request) {
        LoginResult result = applicationService.logIn(mapper.loginCommand(request));
        return mapper.sessionResponse(result);
    }

    /** Ends the represented session; an already-ended session is treated as successfully ended. */
    public void logOut(Actor actor, String refreshToken) {
        applicationService.logOut(toCallerContext(actor), refreshToken);
    }

    /** Invalidates every active refresh-token session belonging to the authenticated account. */
    public void endAllOwnSessions(Actor actor) {
        applicationService.endAllOwnSessions(toCallerContext(actor));
    }

    /** Rotates a refresh token and returns a session carrying the account's current roles. */
    public SessionResponse renewSession(RenewSessionRequest request) {
        LoginResult result = applicationService.renewSession(request.refreshToken());
        return mapper.sessionResponse(result);
    }

    /** Changes the caller's password and, by default, ends other active sessions. */
    public void changeOwnPassword(Actor actor, ChangePasswordRequest request) {
        boolean endOtherSessions = request.endOtherSessions() == null || request.endOtherSessions();
        applicationService.changeOwnPassword(toCallerContext(actor),
            new ChangePasswordCommand(request.currentPassword(), request.newPassword(), endOtherSessions));
    }

    /** Starts password-reset delivery without revealing whether the supplied email is registered. */
    public void requestPasswordReset(PasswordResetRequestRequest request) {
        applicationService.requestPasswordReset(request.email());
    }

    /** Completes password reset with a single-use reset token. */
    public void completePasswordReset(PasswordResetCompletionRequest request) {
        applicationService.completePasswordReset(request.token(), request.newPassword());
    }

    /** Returns the authenticated account's current profile. */
    public AccountView getOwnAccount(Actor actor) {
        return mapper.accountView(applicationService.getOwnAccount(toCallerContext(actor)));
    }

    /** Applies the supplied profile changes to the authenticated account. */
    public AccountView updateOwnProfile(Actor actor, ProfileUpdateRequest request) {
        AccountSummary result = applicationService.updateOwnProfile(toCallerContext(actor), mapper.updateProfileCommand(request));
        return mapper.accountView(result);
    }

    /** Lists only the authenticated account's addresses with opaque cursor pagination. */
    public AddressPageView listOwnAddresses(Actor actor, String cursor, int size) {
        AddressPageResult result = applicationService.listOwnAddresses(toCallerContext(actor), cursor, size);
        return new AddressPageView(result.items().stream().map(mapper::addressView).toList(),
            result.nextCursor());
    }

    /** Adds an address for the authenticated account and maintains its single-default invariant. */
    public AddressView addOwnAddress(Actor actor, AddressWriteRequest request) {
        return mapper.addressView(applicationService.addOwnAddress(toCallerContext(actor), mapper.addressCommand(request)));
    }

    /** Returns an address only when it belongs to the authenticated account. */
    public AddressView getOwnAddress(Actor actor, UUID addressId) {
        return mapper.addressView(applicationService.getOwnAddress(toCallerContext(actor), addressId));
    }

    /** Replaces all mutable fields of an address owned by the authenticated account. */
    public AddressView replaceOwnAddress(Actor actor, UUID addressId, AddressWriteRequest request) {
        return mapper.addressView(applicationService.replaceOwnAddress(toCallerContext(actor), addressId,
            mapper.addressCommand(request)));
    }

    /** Removes an address only when it belongs to the authenticated account. */
    public void removeOwnAddress(Actor actor, UUID addressId) {
        applicationService.removeOwnAddress(toCallerContext(actor), addressId);
    }

    private static CallerContext toCallerContext(Actor actor) {
        return CallerContext.of(actor.accountId(), actor.roles());
    }
}
