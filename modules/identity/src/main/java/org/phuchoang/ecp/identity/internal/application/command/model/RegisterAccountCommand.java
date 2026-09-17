package org.phuchoang.ecp.identity.internal.application.command.model;

/** Normalized registration input for {@code UC-CUS-01}, before account and verification-token creation. */
public record RegisterAccountCommand(String email, String password, String displayName) {
}
