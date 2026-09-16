package org.phuchoang.ecp.identity.api.request;

/** `verifyEmailAddress` — `components/schemas/identity.yaml#/EmailVerificationRequest`. */
public record EmailVerificationRequest(String token) {
}
