package org.phuchoang.ecp.identity.api;

import java.time.Instant;
import java.util.Set;

/** `components/schemas/identity.yaml#/Account`. */
public record AccountView(
    String id,
    String email,
    String status,
    String verificationStatus,
    Set<String> roles,
    Instant verifiedAt,
    Instant lastLoginAt,
    Instant createdAt) {
}
