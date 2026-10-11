package org.phuchoang.ecp.web.identity;

import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.ProfileUpdateRequest;
import org.phuchoang.ecp.identity.api.view.AccountView;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** `getOwnAccount`, `updateOwnProfile` (`UC-CUS-08`). */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class ProfileController {

    private final IdentityFacade identityFacade;
    private final RequestContextResolver requestContext;

    @GetMapping("/api/v1/accounts/me")
    AccountView getOwnAccount(@AuthenticationPrincipal Jwt jwt) {
        return identityFacade.getOwnAccount(requestContext.resolve(jwt).caller());
    }

    @PatchMapping("/api/v1/accounts/me")
    AccountView updateOwnProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody ProfileUpdateRequest request) {
        return identityFacade.updateOwnProfile(requestContext.resolve(jwt).caller(), request);
    }
}
