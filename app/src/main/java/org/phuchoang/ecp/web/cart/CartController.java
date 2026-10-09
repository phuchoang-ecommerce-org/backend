package org.phuchoang.ecp.web.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletResponse;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartLineWrite;
import org.phuchoang.ecp.cart.api.CartView;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** HTTP and cookie adapter; Cart's application layer receives only resolved ownership identity. */
@RestController
class CartController {
    static final String GUEST_CART_COOKIE = "ecp_guest_cart";
    private final CartFacade carts;
    private final RequestContextResolver requestContext;
    CartController(CartFacade carts, RequestContextResolver requestContext) { this.carts = carts; this.requestContext = requestContext; }

    @GetMapping("/api/v1/carts/current")
    CartView current(@AuthenticationPrincipal Jwt jwt, @CookieValue(name = GUEST_CART_COOKIE, required = false) String cookie,
                     HttpServletResponse response) {
        RequestContext context = requestContext.resolve(jwt);
        return carts.getCurrentCart(context.caller(), guestToken(context.caller(), cookie, response));
    }

    @GetMapping("/api/v1/carts/{cartId}")
    CartView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cartId,
                 @CookieValue(name = GUEST_CART_COOKIE, required = false) String cookie, HttpServletResponse response) {
        RequestContext context = requestContext.resolve(jwt);
        return carts.getCart(context.caller(), guestToken(context.caller(), cookie, response), cartId);
    }

    @PostMapping("/api/v1/carts/{cartId}/lines")
    CartView add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cartId, @Valid @RequestBody CartLineWrite write,
                 @CookieValue(name = GUEST_CART_COOKIE, required = false) String cookie, HttpServletResponse response) {
        RequestContext context = requestContext.resolve(jwt);
        return carts.addCartLine(context.caller(), guestToken(context.caller(), cookie, response), cartId, write);
    }

    @PatchMapping("/api/v1/carts/{cartId}/lines/{lineId}")
    CartView update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cartId, @PathVariable UUID lineId,
                    @Valid @RequestBody QuantityWrite write, @CookieValue(name = GUEST_CART_COOKIE, required = false) String cookie,
                    HttpServletResponse response) {
        RequestContext context = requestContext.resolve(jwt);
        return carts.updateCartLineQuantity(context.caller(), guestToken(context.caller(), cookie, response), cartId, lineId, write.quantity());
    }

    @DeleteMapping("/api/v1/carts/{cartId}/lines/{lineId}")
    void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cartId, @PathVariable UUID lineId,
                @CookieValue(name = GUEST_CART_COOKIE, required = false) String cookie, HttpServletResponse response) {
        RequestContext context = requestContext.resolve(jwt);
        carts.removeCartLine(context.caller(), guestToken(context.caller(), cookie, response), cartId, lineId);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static String guestToken(IdentityActor caller, String existing, HttpServletResponse response) {
        if (caller != null && caller.accountId() != null) return null;
        if (existing != null && !existing.isBlank()) return existing;
        String generated = UUID.randomUUID().toString();
        response.addHeader("Set-Cookie", ResponseCookie.from(GUEST_CART_COOKIE, generated).httpOnly(true).secure(true)
            .sameSite("Lax").path("/api/v1/carts").maxAge(java.time.Duration.ofDays(7)).build().toString());
        return generated;
    }

    record QuantityWrite(@Min(0) int quantity) { }
}
