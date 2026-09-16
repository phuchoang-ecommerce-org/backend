package org.phuchoang.ecp.identity.api.request;

/** `registerAccount` — `components/schemas/identity.yaml#/RegistrationRequest`. */
public record RegisterAccountRequest(String email, String password, String displayName) {
}
