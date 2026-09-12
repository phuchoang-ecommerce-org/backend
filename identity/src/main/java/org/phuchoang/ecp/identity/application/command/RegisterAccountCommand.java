package org.phuchoang.ecp.identity.application.command;

public record RegisterAccountCommand(String email, String password, String displayName) {
}
