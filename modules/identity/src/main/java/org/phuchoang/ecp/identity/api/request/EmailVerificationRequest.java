package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `verifyEmailAddress` — `components/schemas/identity.yaml#/EmailVerificationRequest`. */
public record EmailVerificationRequest(@NotBlank(message = "token is required.") String token) {
}
