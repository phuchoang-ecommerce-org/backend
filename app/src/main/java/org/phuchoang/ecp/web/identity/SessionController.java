package org.phuchoang.ecp.web.identity;

import jakarta.validation.Valid;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartMergeNotice;
import org.phuchoang.ecp.cart.api.CartMergeResult;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.LoginRequest;
import org.phuchoang.ecp.identity.api.request.LogoutRequest;
import org.phuchoang.ecp.identity.api.request.RenewSessionRequest;
import org.phuchoang.ecp.identity.api.view.SessionResponse;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

/** `logIn`, `logOut`, `endAllOwnSessions` (`UC-CUS-03`, `UC-CUS-04`), `renewSession` (`UC-CUS-05`). */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class SessionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

    private final IdentityFacade identityFacade;
    private final CartFacade carts;
    private final RequestContextResolver requestContext;

    @PostMapping("/api/v1/sessions")
    ResponseEntity<SessionResponse> logIn(@Valid @RequestBody LoginRequest request) {
        SessionResponse session = identityFacade.logIn(request);
        if (request.guestCartId() != null) {
            try {
                CartMergeResult merge = carts.mergeGuestCart(UUID.fromString(session.account().id()), request.guestCartId().toString());
                session = withCartMergeOutcome(session, merge);
            } catch (RuntimeException exception) {
                // UC-CUS-03 E4: session issuance has already succeeded; preserve the guest cart for retry.
                LOGGER.warn("Guest cart merge failed after session issuance: {}", exception.getClass().getSimpleName());
                session = new SessionResponse(session.accessToken(), session.refreshToken(), session.expiresIn(),
                    session.restricted(), session.account(), new SessionResponse.CartMergeOutcome(false, null, null));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/sessions/current"))
            .body(session);
    }

    private static SessionResponse withCartMergeOutcome(SessionResponse session, CartMergeResult merge) {
        return new SessionResponse(session.accessToken(), session.refreshToken(), session.expiresIn(), session.restricted(),
            session.account(), new SessionResponse.CartMergeOutcome(merge.merged(), null,
                merge.notices().stream().map(SessionController::adjustment).toList()));
    }

    private static SessionResponse.CartMergeAdjustment adjustment(CartMergeNotice notice) {
        return switch (notice.reason()) {
            case DROPPED_UNPUBLISHED -> new SessionResponse.CartMergeAdjustment(
                notice.variantId(), "DROPPED", null, null, null);
            case QUANTITY_REDUCED_TO_AVAILABLE -> new SessionResponse.CartMergeAdjustment(
                notice.variantId(), "QUANTITY_CAPPED", null, notice.quantity(), null);
            case RETAINED_OUT_OF_STOCK -> new SessionResponse.CartMergeAdjustment(
                notice.variantId(), "CARRIED_UNPURCHASABLE", notice.quantity(), notice.quantity(), null);
        };
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
    SessionResponse renewSession(@Valid @RequestBody RenewSessionRequest request) {
        return identityFacade.renewSession(request);
    }
}
