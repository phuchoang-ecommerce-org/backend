package org.phuchoang.ecp.identity.api.facade;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
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
import org.phuchoang.ecp.identity.internal.application.address.AddressPageResult;
import org.phuchoang.ecp.identity.internal.application.address.AddressUseCases;
import org.phuchoang.ecp.identity.internal.application.authentication.AuthenticationUseCases;
import org.phuchoang.ecp.identity.internal.application.authentication.LoginResult;
import org.phuchoang.ecp.identity.internal.application.password.ChangePasswordCommand;
import org.phuchoang.ecp.identity.internal.application.password.PasswordUseCases;
import org.phuchoang.ecp.identity.internal.application.profile.AccountSummary;
import org.phuchoang.ecp.identity.internal.application.profile.ProfileUseCases;
import org.phuchoang.ecp.identity.internal.application.registration.RegistrationUseCases;
import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The reachable surface of the {@code identity} module (`@NamedInterface`). {@code app}'s web
 * layer calls only this — never {@code identity.internal.application} directly
 * (`noClassReachesIntoAnotherModulesApplicationPackage`). Thin by design: every method converts
 * the external {@link IdentityActor} into the internal {@link CallerContext} exactly once,
 * translates between wire-shaped api records and application commands/results, and delegates to
 * the capability that owns the workflow.
 */
@Component
public final class IdentityFacade {

    private final RegistrationUseCases registration;
    private final AuthenticationUseCases authentication;
    private final PasswordUseCases passwords;
    private final ProfileUseCases profiles;
    private final AddressUseCases addresses;
    private final IdentityDtoMapper mapper;

    IdentityFacade(RegistrationUseCases registration, AuthenticationUseCases authentication,
            PasswordUseCases passwords, ProfileUseCases profiles, AddressUseCases addresses,
            IdentityDtoMapper mapper) {
        this.registration = registration;
        this.authentication = authentication;
        this.passwords = passwords;
        this.profiles = profiles;
        this.addresses = addresses;
        this.mapper = mapper;
    }

    /** Registers an account and starts its email-verification flow ({@code UC-CUS-01}). */
    public void registerAccount(RegisterAccountRequest request) {
        registration.registerAccount(mapper.registerAccountCommand(request));
    }

    /** Proves ownership of a pending email address using its single-use verification token. */
    public void verifyEmailAddress(String token) {
        registration.verifyEmailAddress(token);
    }

    /** Invalidates a prior verification token and requests delivery of a replacement. */
    public void resendEmailVerification(String email) {
        registration.resendEmailVerification(email);
    }

    /** Authenticates credentials and returns a new access/refresh session pair. */
    public SessionResponse logIn(LoginRequest request) {
        LoginResult result = authentication.logIn(mapper.loginCommand(request));
        return mapper.sessionResponse(result);
    }

    /** Ends the represented session; an already-ended session is treated as successfully ended. */
    public void logOut(IdentityActor actor, String refreshToken) {
        authentication.logOut(toCaller(actor), refreshToken);
    }

    /** Invalidates every active refresh-token session belonging to the authenticated account. */
    public void endAllOwnSessions(IdentityActor actor) {
        authentication.endAllOwnSessions(toCaller(actor));
    }

    /** Rotates a refresh token and returns a session carrying the account's current roles. */
    public SessionResponse renewSession(RenewSessionRequest request) {
        LoginResult result = authentication.renewSession(request.refreshToken());
        return mapper.sessionResponse(result);
    }

    /** Changes the caller's password and, by default, ends other active sessions. */
    public void changeOwnPassword(IdentityActor actor, ChangePasswordRequest request) {
        boolean endOtherSessions = request.endOtherSessions() == null || request.endOtherSessions();
        passwords.changeOwnPassword(toCaller(actor),
            new ChangePasswordCommand(request.currentPassword(), request.newPassword(), endOtherSessions));
    }

    /** Starts password-reset delivery without revealing whether the supplied email is registered. */
    public void requestPasswordReset(PasswordResetRequestRequest request) {
        passwords.requestPasswordReset(request.email());
    }

    /** Completes password reset with a single-use reset token. */
    public void completePasswordReset(PasswordResetCompletionRequest request) {
        passwords.completePasswordReset(request.token(), request.newPassword());
    }

    /** Returns the authenticated account's current profile. */
    public AccountView getOwnAccount(IdentityActor actor) {
        return mapper.accountView(profiles.getOwnAccount(toCaller(actor)));
    }

    /** Applies the supplied profile changes to the authenticated account. */
    public AccountView updateOwnProfile(IdentityActor actor, ProfileUpdateRequest request) {
        AccountSummary result = profiles.updateOwnProfile(toCaller(actor), mapper.updateProfileCommand(request));
        return mapper.accountView(result);
    }

    /** Lists only the authenticated account's addresses with opaque cursor pagination. */
    public AddressPageView listOwnAddresses(IdentityActor actor, String cursor, int size) {
        AddressPageResult result = addresses.listOwnAddresses(toCaller(actor), cursor, size);
        return new AddressPageView(result.items().stream().map(mapper::addressView).toList(), result.nextCursor());
    }

    /** Adds an address for the authenticated account and maintains its single-default invariant. */
    public AddressView addOwnAddress(IdentityActor actor, AddressWriteRequest request) {
        return mapper.addressView(addresses.addOwnAddress(toCaller(actor), mapper.addressCommand(request)));
    }

    /** Returns an address only when it belongs to the authenticated account. */
    public AddressView getOwnAddress(IdentityActor actor, UUID addressId) {
        return mapper.addressView(addresses.getOwnAddress(toCaller(actor), addressId));
    }

    /** Replaces all mutable fields of an address owned by the authenticated account. */
    public AddressView replaceOwnAddress(IdentityActor actor, UUID addressId, AddressWriteRequest request) {
        return mapper.addressView(addresses.replaceOwnAddress(toCaller(actor), addressId, mapper.addressCommand(request)));
    }

    /** Removes an address only when it belongs to the authenticated account. */
    public void removeOwnAddress(IdentityActor actor, UUID addressId) {
        addresses.removeOwnAddress(toCaller(actor), addressId);
    }

    private static CallerContext toCaller(IdentityActor actor) {
        return CallerContext.of(actor.accountId(), actor.roles());
    }
}
