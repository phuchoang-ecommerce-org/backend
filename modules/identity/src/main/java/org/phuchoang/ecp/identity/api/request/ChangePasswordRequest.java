package org.phuchoang.ecp.identity.api.request;

/** `components/schemas/identity.yaml#/PasswordChangeRequest`. */
public record ChangePasswordRequest(String currentPassword, String newPassword, Boolean endOtherSessions) {
}
