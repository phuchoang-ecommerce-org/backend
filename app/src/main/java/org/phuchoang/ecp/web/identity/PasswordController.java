package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.Actor;
import org.phuchoang.ecp.identity.api.ChangePasswordRequest;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.identity.api.PasswordResetCompletionRequest;
import org.phuchoang.ecp.identity.api.PasswordResetRequestRequest;
import org.phuchoang.ecp.sharedkernel.api.FieldErrorCodes;
import org.phuchoang.ecp.web.FieldError;
import org.phuchoang.ecp.web.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** `changeOwnPassword` (`UC-CUS-06`), `requestPasswordReset`/`completePasswordReset` (`UC-CUS-07`). */
@RestController
class PasswordController {

    private final IdentityFacade identityFacade;

    PasswordController(IdentityFacade identityFacade) {
        this.identityFacade = identityFacade;
    }

    @PutMapping("/api/v1/accounts/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeOwnPassword(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest request) {
        requireNonBlank(request.currentPassword(), "currentPassword");
        requireNonBlank(request.newPassword(), "newPassword");
        identityFacade.changeOwnPassword(actorOf(jwt), request);
    }

    @PostMapping("/api/v1/password-reset-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@RequestBody PasswordResetRequestRequest request) {
        requireNonBlank(request.email(), "email");
        identityFacade.requestPasswordReset(request);
    }

    @PostMapping("/api/v1/password-resets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completePasswordReset(@RequestBody PasswordResetCompletionRequest request) {
        requireNonBlank(request.token(), "token");
        requireNonBlank(request.newPassword(), "newPassword");
        identityFacade.completePasswordReset(request);
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            List<FieldError> errors = new ArrayList<>();
            errors.add(new FieldError(field, FieldErrorCodes.REQUIRED, field + " is required."));
            throw new ValidationException(errors);
        }
    }

    private Actor actorOf(Jwt jwt) {
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles"));
        return new Actor(UUID.fromString(jwt.getSubject()), roles);
    }
}
