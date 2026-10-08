package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `components/schemas/identity.yaml#/PasswordChangeRequest`. */
public record ChangePasswordRequest(@NotBlank(message = "currentPassword is required.") String currentPassword,
        @NotBlank(message = "newPassword is required.") String newPassword, Boolean endOtherSessions) {
}
