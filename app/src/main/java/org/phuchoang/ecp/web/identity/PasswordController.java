package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.ChangePasswordRequest;
import org.phuchoang.ecp.identity.api.request.PasswordResetCompletionRequest;
import org.phuchoang.ecp.identity.api.request.PasswordResetRequestRequest;
import org.phuchoang.ecp.web.common.request.RequestValidation;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** `changeOwnPassword` (`UC-CUS-06`), `requestPasswordReset`/`completePasswordReset` (`UC-CUS-07`). */
@RestController
class PasswordController {

    private final IdentityFacade identityFacade;
    private final RequestContextResolver requestContext;

    PasswordController(IdentityFacade identityFacade, RequestContextResolver requestContext) {
        this.identityFacade = identityFacade;
        this.requestContext = requestContext;
    }

    @PutMapping("/api/v1/accounts/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeOwnPassword(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePasswordRequest request) {
        RequestValidation.requireNonBlank(request.currentPassword(), "currentPassword");
        RequestValidation.requireNonBlank(request.newPassword(), "newPassword");

        identityFacade.changeOwnPassword(requestContext.resolve(jwt).caller(), request);
    }

    @PostMapping("/api/v1/password-reset-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@RequestBody PasswordResetRequestRequest request) {
        RequestValidation.requireNonBlank(request.email(), "email");

        identityFacade.requestPasswordReset(request);
    }

    @PostMapping("/api/v1/password-resets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completePasswordReset(@RequestBody PasswordResetCompletionRequest request) {
        RequestValidation.requireNonBlank(request.token(), "token");
        RequestValidation.requireNonBlank(request.newPassword(), "newPassword");

        identityFacade.completePasswordReset(request);
    }
}
