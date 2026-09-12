package org.phuchoang.ecp.identity.api;

/** `components/schemas/identity.yaml#/PasswordReset`. */
public record PasswordResetCompletionRequest(String token, String newPassword) {
}
