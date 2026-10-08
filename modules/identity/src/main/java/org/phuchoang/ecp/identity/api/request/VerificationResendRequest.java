package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `resendEmailVerification` — `components/schemas/identity.yaml#/VerificationResendRequest`. */
public record VerificationResendRequest(@NotBlank(message = "email is required.") String email) {
}
