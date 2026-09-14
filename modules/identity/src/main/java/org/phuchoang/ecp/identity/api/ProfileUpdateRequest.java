package org.phuchoang.ecp.identity.api;

/** `components/schemas/identity.yaml#/ProfileUpdate`. {@code null} means "unchanged". */
public record ProfileUpdateRequest(String displayName, String email) {
}
