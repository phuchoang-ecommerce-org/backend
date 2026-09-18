package org.phuchoang.ecp.identity.api.authorization;

import org.phuchoang.ecp.identity.internal.application.security.CallerContext;
import org.phuchoang.ecp.identity.internal.application.security.PermissionChecker;
import org.springframework.stereotype.Component;

/**
 * Exposes the internal {@link PermissionChecker} as this module's {@code api}-level
 * {@link IdentityAuthorization} (`US-AUD-03`). identity's own use-case services inject the
 * internal port directly; this adapter exists only for other modules' application layers, which
 * cannot see {@code identity.internal.application} at all
 * (`noClassReachesIntoAnotherModulesApplicationPackage`).
 *
 * <p>Lives in {@code api}, not {@code application}: an {@code application}-layer class may not
 * implement an {@code api}-layer interface (`applicationDoesNotDependOnItsOwnInfrastructureOrApi`),
 * but {@code api} may depend on {@code application}, so the {@code IdentityActor -> CallerContext}
 * translation happens exactly once, here, on this side of the boundary.
 */
@Component
final class IdentityAuthorizationAdapter implements IdentityAuthorization {

    private final PermissionChecker permissions;

    IdentityAuthorizationAdapter(PermissionChecker permissions) {
        this.permissions = permissions;
    }

    @Override
    public void assertAuthorized(IdentityActor caller, String operationId) {
        permissions.require(CallerContext.of(caller.accountId(), caller.roles()), operationId);
    }
}
