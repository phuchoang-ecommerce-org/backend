package org.phuchoang.ecp.identity.domain;

import java.util.Optional;

/**
 * Password strength policy (`NFR-SEC-04`). `UC-CUS-01` §"Assumptions" notes no strength policy is
 * specified by R1 — this is a deliberate, documented interim choice, not a rule pulled from a
 * spec: at least 10 characters, one upper-case letter, one lower-case letter, one digit. It is
 * enforced here, in the domain, never in a controller (Sprint 03 backlog, `US-CUS-01`).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;

    private PasswordPolicy() {
    }

    /** Returns the first unmet criterion, or empty if {@code rawPassword} satisfies the policy. */
    public static Optional<String> firstViolation(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            return Optional.of("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (rawPassword.chars().noneMatch(Character::isUpperCase)) {
            return Optional.of("Password must contain an upper-case letter.");
        }
        if (rawPassword.chars().noneMatch(Character::isLowerCase)) {
            return Optional.of("Password must contain a lower-case letter.");
        }
        if (rawPassword.chars().noneMatch(Character::isDigit)) {
            return Optional.of("Password must contain a digit.");
        }
        return Optional.empty();
    }

    public static boolean satisfies(String rawPassword) {
        return firstViolation(rawPassword).isEmpty();
    }
}
