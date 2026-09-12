package org.phuchoang.ecp.identity.application.command;

/** {@code null} in either field means "unchanged" (`UC-CUS-08` A2). */
public record UpdateProfileCommand(String displayName, String email) {
}
