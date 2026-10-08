package org.phuchoang.ecp.web.identity;

import jakarta.validation.Valid;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.EmailVerificationRequest;
import org.phuchoang.ecp.identity.api.request.VerificationResendRequest;
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
    void verifyEmailAddress(@Valid @RequestBody EmailVerificationRequest request) {
        identityFacade.verifyEmailAddress(request.token());
    }

    @PostMapping("/api/v1/account-verification-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendEmailVerification(@Valid @RequestBody VerificationResendRequest request) {
        identityFacade.resendEmailVerification(request.email());
    }
}
