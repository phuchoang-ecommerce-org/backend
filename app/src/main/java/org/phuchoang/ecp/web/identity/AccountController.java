package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.RegisterAccountRequest;
import org.phuchoang.ecp.sharedkernel.api.error.FieldErrorCodes;
import org.phuchoang.ecp.web.error.FieldError;
import org.phuchoang.ecp.web.error.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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
        requireNonBlank(request.email(), "email");
        requireNonBlank(request.password(), "password");
        identityFacade.registerAccount(request);
    }

    static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            List<FieldError> errors = new ArrayList<>();
            errors.add(new FieldError(field, FieldErrorCodes.REQUIRED, field + " is required."));
            throw new ValidationException(errors);
        }
    }
}
