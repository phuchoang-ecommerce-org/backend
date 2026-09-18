package org.phuchoang.ecp.identity.internal.application.security;

import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * {@link PermissionChecker} backed by the hardcoded {@link PermissionMatrix} (`ADR-0016` §4). A
 * denial is `ECP-GEN-4030`, never a disclosure of <em>why</em> — the caller learns only that the
 * role does not permit the operation.
 */
@Service
public class PermissionMatrixPermissionChecker implements PermissionChecker {

    @Override
    public void require(CallerContext caller, String operationId) {
        Set<RoleCode> allowed = PermissionMatrix.allowedRoles(operationId);
        boolean permitted = caller.roles().stream().anyMatch(allowed::contains);
        if (!permitted) {
            throw new DomainException(GenErrorCode.FORBIDDEN,
                "The caller's role does not permit operation " + operationId + ".");
        }
    }
}
