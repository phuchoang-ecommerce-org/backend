package org.phuchoang.ecp.identity.internal.application.command.model;

/** Credentials presented for {@code UC-CUS-03}; callers receive no indication of which value failed. */
public record LoginCommand(String email, String password) {
}
