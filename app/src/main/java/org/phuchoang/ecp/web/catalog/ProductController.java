package org.phuchoang.ecp.web.catalog;

import org.phuchoang.ecp.catalog.api.GetProductFacade;
import org.phuchoang.ecp.catalog.api.ProductView;
import org.phuchoang.ecp.identity.api.CallerContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/**
 * Sprint 04 (`US-AUD-03`) RBAC cross-module wiring demo only — see
 * {@code catalog.application.GetProductService}'s Javadoc. Not a real catalog endpoint.
 */
@RestController
class ProductController {

    private final GetProductFacade getProductFacade;

    ProductController(GetProductFacade getProductFacade) {
        this.getProductFacade = getProductFacade;
    }

    @GetMapping("/api/v1/demo-products/{productId}")
    ProductView getProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        return getProductFacade.getProduct(callerOf(jwt), productId);
    }

    private static CallerContext callerOf(Jwt jwt) {
        if (jwt == null) {
            return CallerContext.GUEST;
        }
        Set<String> roles = Set.copyOf(jwt.getClaimAsStringList("roles"));
        return new CallerContext(UUID.fromString(jwt.getSubject()), roles);
    }
}
