package org.phuchoang.ecp.identity.application.command;

public record ChangePasswordCommand(String currentPassword, String newPassword, boolean endOtherSessions) {
}
