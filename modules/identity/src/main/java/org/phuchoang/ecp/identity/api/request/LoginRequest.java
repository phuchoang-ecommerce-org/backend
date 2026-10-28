package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** `logIn` — `components/schemas/identity.yaml#/CredentialsRequest`. */
public record LoginRequest(@NotBlank(message = "email is required.") String email,
        @NotBlank(message = "password is required.") String password, UUID guestCartId) {
}
