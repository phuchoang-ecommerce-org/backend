package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `renewSession` (`UC-CUS-05`) — `POST /session-renewals`, Permission Matrix.md §5.1. */
public record RenewSessionRequest(@NotBlank(message = "refreshToken is required.") String refreshToken) {
}
