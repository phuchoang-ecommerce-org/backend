package org.phuchoang.ecp.identity.api;

/** `renewSession` (`UC-CUS-05`) — `POST /session-renewals`, Permission Matrix.md §5.1. */
public record RenewSessionRequest(String refreshToken) {
}
