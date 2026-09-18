package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.LoginRequest;
import org.phuchoang.ecp.identity.api.request.LogoutRequest;
import org.phuchoang.ecp.identity.api.request.RenewSessionRequest;
import org.phuchoang.ecp.identity.api.view.SessionResponse;
import org.phuchoang.ecp.web.common.request.RequestValidation;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** `logIn`, `logOut`, `endAllOwnSessions` (`UC-CUS-03`, `UC-CUS-04`), `renewSession` (`UC-CUS-05`). */
@RestController
class SessionController {

    private final IdentityFacade identityFacade;
    private final RequestContextResolver requestContext;

    SessionController(IdentityFacade identityFacade, RequestContextResolver requestContext) {
        this.identityFacade = identityFacade;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/v1/sessions")
    ResponseEntity<SessionResponse> logIn(@RequestBody LoginRequest request) {
        RequestValidation.requireNonBlank(request.email(), "email");
        RequestValidation.requireNonBlank(request.password(), "password");

        SessionResponse session = identityFacade.logIn(request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/sessions/current"))
            .body(session);
    }

    @DeleteMapping("/api/v1/sessions/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logOut(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = request == null ? null : request.refreshToken();

        identityFacade.logOut(requestContext.resolve(jwt).caller(), refreshToken);
    }

    @DeleteMapping("/api/v1/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void endAllOwnSessions(@AuthenticationPrincipal Jwt jwt) {
        identityFacade.endAllOwnSessions(requestContext.resolve(jwt).caller());
    }

    /**
     * No {@code @AuthenticationPrincipal} — the caller's access token is, by definition, expired or
     * absent here; only the refresh token in the body authenticates this call (see
     * {@code SecurityConfig}'s anonymous allowlist). 200, not 201: this renews the existing session
     * in place (openapi.yaml `sessionRenewals`), unlike `logIn`, which creates a brand-new one.
     */
    @PostMapping("/api/v1/session-renewals")
    SessionResponse renewSession(@RequestBody RenewSessionRequest request) {
        RequestValidation.requireNonBlank(request.refreshToken(), "refreshToken");

        return identityFacade.renewSession(request);
    }
}
