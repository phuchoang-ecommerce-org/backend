package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `components/schemas/identity.yaml#/PasswordReset`. */
public record PasswordResetCompletionRequest(@NotBlank(message = "token is required.") String token,
        @NotBlank(message = "newPassword is required.") String newPassword) {
}
