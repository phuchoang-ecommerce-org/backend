package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.Actor;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.identity.api.LoginRequest;
import org.phuchoang.ecp.identity.api.LogoutRequest;
import org.phuchoang.ecp.identity.api.SessionResponse;
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
import java.util.Set;
import java.util.UUID;

/** `logIn`, `logOut`, `endAllOwnSessions` (`UC-CUS-03`, `UC-CUS-04`). */
@RestController
class SessionController {

    private final IdentityFacade identityFacade;

    SessionController(IdentityFacade identityFacade) {
        this.identityFacade = identityFacade;
    }

    @PostMapping("/api/v1/sessions")
    ResponseEntity<SessionResponse> logIn(@RequestBody LoginRequest request) {
        AccountController.requireNonBlank(request.email(), "email");
        AccountController.requireNonBlank(request.password(), "password");
        SessionResponse session = identityFacade.logIn(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/sessions/current"))
            .body(session);
    }

    @DeleteMapping("/api/v1/sessions/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logOut(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) LogoutRequest request) {
        String refreshToken = request == null ? null : request.refreshToken();
        identityFacade.logOut(actorOf(jwt), refreshToken);
    }

    @DeleteMapping("/api/v1/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void endAllOwnSessions(@AuthenticationPrincipal Jwt jwt) {
        identityFacade.endAllOwnSessions(actorOf(jwt));
    }

    private Actor actorOf(Jwt jwt) {
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles"));
        return new Actor(UUID.fromString(jwt.getSubject()), roles);
    }
}
