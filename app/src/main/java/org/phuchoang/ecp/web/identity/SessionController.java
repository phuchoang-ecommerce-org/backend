package org.phuchoang.ecp.web.identity;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartMergeNotice;
import org.phuchoang.ecp.cart.api.CartMergeResult;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.LoginRequest;
import org.phuchoang.ecp.identity.api.request.LogoutRequest;
import org.phuchoang.ecp.identity.api.request.RenewSessionRequest;
import org.phuchoang.ecp.identity.api.view.SessionResponse;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** `logIn`, `logOut`, `endAllOwnSessions` (`UC-CUS-03`, `UC-CUS-04`), `renewSession` (`UC-CUS-05`). */
@RestController
class SessionController {
    private static final String GUEST_CART_COOKIE = "ecp_guest_cart";

    private final IdentityFacade identityFacade;
    private final CartFacade carts;
    private final RequestContextResolver requestContext;

    SessionController(IdentityFacade identityFacade, CartFacade carts, RequestContextResolver requestContext) {
        this.identityFacade = identityFacade;
        this.carts = carts;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/v1/sessions")
    ResponseEntity<SessionResponse> logIn(@Valid @RequestBody LoginRequest request,
            @CookieValue(name = GUEST_CART_COOKIE, required = false) String guestToken, HttpServletResponse response) {
        SessionResponse session = identityFacade.logIn(request);
        if (guestToken != null && !guestToken.isBlank()) {
            try {
                CartMergeResult merge = carts.mergeGuestCart(UUID.fromString(session.account().id()), guestToken);
                session = withCartMergeNotices(session, merge.notices().stream().map(SessionController::notice).toList());
                if (merge.merged()) clearGuestCartCookie(response);
            } catch (RuntimeException exception) {
                // UC-CUS-03 E4: session issuance has already succeeded; preserve the cookie for retry.
                session = withCartMergeNotices(session, List.of("Your guest cart could not be recovered yet; retrying sign-in will recover your items."));
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/sessions/current"))
            .body(session);
    }

    private static String notice(CartMergeNotice notice) {
        String item = notice.productName() == null || notice.productName().isBlank() ? notice.sku() : notice.productName();
        return switch (notice.reason()) {
            case DROPPED_UNPUBLISHED -> item + " was not carried over because it is no longer published.";
            case QUANTITY_REDUCED_TO_AVAILABLE -> item + " was reduced to " + notice.quantity() + " because of available stock.";
            case RETAINED_OUT_OF_STOCK -> item + " was carried over but is currently out of stock.";
        };
    }

    private static SessionResponse withCartMergeNotices(SessionResponse session, List<String> notices) {
        return new SessionResponse(session.accessToken(), session.refreshToken(), session.expiresIn(), session.restricted(),
            session.account(), notices);
    }

    private static void clearGuestCartCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from(GUEST_CART_COOKIE, "").httpOnly(true).secure(true)
            .sameSite("Lax").path("/api/v1/carts").maxAge(0).build().toString());
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
