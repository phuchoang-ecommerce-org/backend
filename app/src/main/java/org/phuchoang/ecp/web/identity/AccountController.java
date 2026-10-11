package org.phuchoang.ecp.web.identity;

import jakarta.validation.Valid;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.RegisterAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** `registerAccount` — `POST /api/v1/accounts` (`UC-CUS-01`). */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class AccountController {

    private final IdentityFacade identityFacade;

    @PostMapping("/api/v1/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void registerAccount(@Valid @RequestBody RegisterAccountRequest request) {
        identityFacade.registerAccount(request);
    }
}
