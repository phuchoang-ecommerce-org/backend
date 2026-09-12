package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.EmailVerificationRequest;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.identity.api.VerificationResendRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** `verifyEmailAddress` and `resendEmailVerification` (`UC-CUS-02`). */
@RestController
class AccountVerificationController {

    private final IdentityFacade identityFacade;

    AccountVerificationController(IdentityFacade identityFacade) {
        this.identityFacade = identityFacade;
    }

    @PostMapping("/api/v1/account-verifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmailAddress(@RequestBody EmailVerificationRequest request) {
        AccountController.requireNonBlank(request.token(), "token");
        identityFacade.verifyEmailAddress(request.token());
    }

    @PostMapping("/api/v1/account-verification-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendEmailVerification(@RequestBody VerificationResendRequest request) {
        AccountController.requireNonBlank(request.email(), "email");
        identityFacade.resendEmailVerification(request.email());
    }
}
