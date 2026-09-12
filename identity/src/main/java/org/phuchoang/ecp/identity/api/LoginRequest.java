package org.phuchoang.ecp.identity.api;

/** `logIn` — `components/schemas/identity.yaml#/CredentialsRequest` (minus `guestCartId`; cart merge is out of scope this sprint). */
public record LoginRequest(String email, String password) {
}
