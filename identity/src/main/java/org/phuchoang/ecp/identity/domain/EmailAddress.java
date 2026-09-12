package org.phuchoang.ecp.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An account's email address. Equality (and therefore {@code ux_identity_account_email}) is
 * case-insensitive (`BR-CUS-01`) — the value object normalises to lower case so any two
 * representations of the same address compare equal wherever it is used as a key.
 */
public final class EmailAddress {

    private static final Pattern SIMPLE_FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String value;

    public EmailAddress(String rawValue) {
        Objects.requireNonNull(rawValue, "email must not be null");
        String normalised = rawValue.trim().toLowerCase(Locale.ROOT);
        if (!SIMPLE_FORMAT.matcher(normalised).matches()) {
            throw new IllegalArgumentException("email is not a valid address");
        }
        this.value = normalised;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EmailAddress that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
