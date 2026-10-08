package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `logIn` — `components/schemas/identity.yaml#/CredentialsRequest` (minus `guestCartId`; cart merge is out of scope this sprint). */
public record LoginRequest(@NotBlank(message = "email is required.") String email,
        @NotBlank(message = "password is required.") String password) {
}
