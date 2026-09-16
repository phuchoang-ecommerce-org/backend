package org.phuchoang.ecp.identity.application.internal;

import org.phuchoang.ecp.identity.application.CallerContext;
import org.phuchoang.ecp.identity.application.port.AuthorizationService;
import org.phuchoang.ecp.identity.domain.RoleCode;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * The `AuthorizationService` OHS, backed by the hardcoded {@link PermissionMatrix}
 * (`ADR-0016` §4). A denial is `ECP-GEN-4030`, never a disclosure of *why* — the caller learns
 * only that the role does not permit the operation.
 */
@Service
public class PermissionMatrixAuthorizationService implements AuthorizationService {

    @Override
    public void assertAuthorized(CallerContext caller, String operationId) {
        Set<RoleCode> allowed = PermissionMatrix.allowedRoles(operationId);
        boolean permitted = caller.roles().stream().anyMatch(allowed::contains);
        if (!permitted) {
            throw new DomainException(GenErrorCode.FORBIDDEN,
                "The caller's role does not permit operation " + operationId + ".");
        }
    }
}
