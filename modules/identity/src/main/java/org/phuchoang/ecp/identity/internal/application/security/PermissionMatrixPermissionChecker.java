package org.phuchoang.ecp.identity.internal.application.security;

import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;
import org.phuchoang.ecp.identity.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.identity.internal.application.error.ApplicationException;
import org.phuchoang.ecp.identity.internal.domain.policy.AccessControlPolicy;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * {@link PermissionChecker} backed by the hardcoded {@link PermissionMatrix} (`ADR-0016` §4). A
 * denial is `ECP-GEN-4030`, never a disclosure of <em>why</em> — the caller learns only that the
 * role does not permit the operation.
 */
@Service
public class PermissionMatrixPermissionChecker implements PermissionChecker {
    private final AccessControlPolicy policy;

    public PermissionMatrixPermissionChecker(AccessControlPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void require(CallerContext caller, String operationId) {
        Set<RoleCode> allowed = PermissionMatrix.allowedRoles(operationId);
        var decision = policy.authorize(new AccessControlPolicy.AccessContext(caller.accountId(), caller.roles(),
            operationId, allowed, null));
        if (!decision.permitted()) {
            throw new ApplicationException(ApplicationErrorCode.FORBIDDEN,
                "The caller's role does not permit operation " + operationId + ".");
        }
    }
}
