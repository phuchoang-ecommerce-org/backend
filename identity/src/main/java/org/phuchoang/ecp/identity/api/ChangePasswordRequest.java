package org.phuchoang.ecp.identity.api;

/** `components/schemas/identity.yaml#/PasswordChangeRequest`. */
public record ChangePasswordRequest(String currentPassword, String newPassword, Boolean endOtherSessions) {
}
