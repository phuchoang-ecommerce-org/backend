package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `components/schemas/identity.yaml#/PasswordResetRequest`. */
public record PasswordResetRequestRequest(@NotBlank(message = "email is required.") String email) {
}
