package org.phuchoang.ecp.identity.application.port;

/**
 * One-way, salted, computationally adaptive password hashing (`NFR-SEC-02`). The plaintext never
 * appears in a log or an exception message anywhere behind this port (`NFR-SEC-07`).
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedHash);
}
