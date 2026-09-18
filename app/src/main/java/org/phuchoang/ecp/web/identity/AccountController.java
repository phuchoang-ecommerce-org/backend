package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.RegisterAccountRequest;
import org.phuchoang.ecp.web.common.request.RequestValidation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** `registerAccount` — `POST /api/v1/accounts` (`UC-CUS-01`). */
@RestController
class AccountController {

    private final IdentityFacade identityFacade;

    AccountController(IdentityFacade identityFacade) {
        this.identityFacade = identityFacade;
    }

    @PostMapping("/api/v1/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void registerAccount(@RequestBody RegisterAccountRequest request) {
        RequestValidation.requireNonBlank(request.email(), "email");
        RequestValidation.requireNonBlank(request.password(), "password");

        identityFacade.registerAccount(request);
    }
}
