package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `registerAccount` — `components/schemas/identity.yaml#/RegistrationRequest`. */
public record RegisterAccountRequest(@NotBlank(message = "email is required.") String email,
        @NotBlank(message = "password is required.") String password, String displayName) {
}
