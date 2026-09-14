package org.phuchoang.ecp.identity.api;

/** `verifyEmailAddress` — `components/schemas/identity.yaml#/EmailVerificationRequest`. */
public record EmailVerificationRequest(String token) {
}
