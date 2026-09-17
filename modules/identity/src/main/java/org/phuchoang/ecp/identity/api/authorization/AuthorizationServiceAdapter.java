package org.phuchoang.ecp.identity.api.authorization;

import org.springframework.stereotype.Component;

/**
 * Exposes the internal {@code identity.application.port.AuthorizationService} OHS (backed by
 * {@code PermissionMatrixAuthorizationService}) as this module's {@code api}-level
 * {@link AuthorizationService} (`US-AUD-03`). identity's own four application services keep
 * injecting the internal port directly; this adapter exists only for other modules' application
 * layers, which cannot see {@code identity.application} at all
 * (`noClassReachesIntoAnotherModulesApplicationPackage`).
 *
 * <p>Lives in {@code api}, not {@code application.internal}: an {@code application}-layer class
 * may not implement an {@code api}-layer interface (`applicationDoesNotDependOnItsOwnInfrastructureOrApi`),
 * but {@code api} may depend on {@code application} (`apiDoesNotDependOnDomainOrInfrastructureInternals`
 * permits exactly that), so the translation has to happen on this side of the boundary.
 */
@Component
final class AuthorizationServiceAdapter implements AuthorizationService {

    private final org.phuchoang.ecp.identity.internal.application.port.AuthorizationService delegate;

    AuthorizationServiceAdapter(org.phuchoang.ecp.identity.internal.application.port.AuthorizationService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void assertAuthorized(CallerContext caller, String operationId) {
        delegate.assertAuthorized(toApplicationCallerContext(caller), operationId);
    }

    private static org.phuchoang.ecp.identity.internal.application.CallerContext toApplicationCallerContext(
            CallerContext caller) {
        return org.phuchoang.ecp.identity.internal.application.CallerContext.of(caller.accountId(), caller.roleNames());
    }
}
