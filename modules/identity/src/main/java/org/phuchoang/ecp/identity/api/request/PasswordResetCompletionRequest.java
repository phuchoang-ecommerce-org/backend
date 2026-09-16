package org.phuchoang.ecp.identity.api.request;

/** `components/schemas/identity.yaml#/PasswordReset`. */
public record PasswordResetCompletionRequest(String token, String newPassword) {
}
