package org.phuchoang.ecp.identity.internal.application.command.model;

/** Input for {@code UC-CUS-06}; the current credential guards a self-service password change. */
public record ChangePasswordCommand(String currentPassword, String newPassword, boolean endOtherSessions) {
}
