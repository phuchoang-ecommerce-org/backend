package org.phuchoang.ecp.identity.internal.domain.policy;

import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Pure role and ownership decision used by Identity's authorization OHS. */
public final class AccessControlPolicy {

    public AuthorizationDecision authorize(AccessContext context) {
        Objects.requireNonNull(context, "context");
        if (context.actorRoles().stream().noneMatch(context.allowedRoles()::contains)) {
            return AuthorizationDecision.denied(Reason.ROLE_NOT_PERMITTED);
        }
        if (context.ownerId() != null && !context.ownerId().equals(context.actorId())) {
            return AuthorizationDecision.denied(Reason.NOT_RESOURCE_OWNER);
        }
        return AuthorizationDecision.allowed();
    }

    public RoleChangeDecision evaluateRoleChange(RoleChangeContext context) {
        Objects.requireNonNull(context, "context");
        if (context.roleToGrant() != null && context.actorId().equals(context.targetId())
                && !context.actorRoles().contains(context.roleToGrant())) {
            return RoleChangeDecision.denied(Reason.SELF_GRANT_NOT_HELD);
        }
        if (context.roleToRevoke() == RoleCode.ADMINISTRATOR && context.targetRoles().contains(RoleCode.ADMINISTRATOR)
                && context.activeAdministratorCount() <= 1) {
            return RoleChangeDecision.denied(Reason.LAST_ADMINISTRATOR);
        }
        return RoleChangeDecision.allowed();
    }

    public record AccessContext(UUID actorId, Set<RoleCode> actorRoles, String operation,
                                Set<RoleCode> allowedRoles, UUID ownerId) {
        public AccessContext {
            actorRoles = Set.copyOf(Objects.requireNonNull(actorRoles, "actorRoles"));
            if (operation == null || operation.isBlank()) throw new IllegalArgumentException("operation is required.");
            allowedRoles = Set.copyOf(Objects.requireNonNull(allowedRoles, "allowedRoles"));
        }
    }
    public record RoleChangeContext(UUID actorId, Set<RoleCode> actorRoles, UUID targetId,
                                    Set<RoleCode> targetRoles, RoleCode roleToGrant,
                                    RoleCode roleToRevoke, long activeAdministratorCount) {
        public RoleChangeContext {
            Objects.requireNonNull(actorId, "actorId"); actorRoles = Set.copyOf(actorRoles);
            Objects.requireNonNull(targetId, "targetId"); targetRoles = Set.copyOf(targetRoles);
            if (activeAdministratorCount < 0) throw new IllegalArgumentException("administrator count cannot be negative.");
        }
    }
    public record AuthorizationDecision(boolean permitted, Reason reason) {
        static AuthorizationDecision allowed() { return new AuthorizationDecision(true, null); }
        static AuthorizationDecision denied(Reason reason) { return new AuthorizationDecision(false, reason); }
    }
    public record RoleChangeDecision(boolean permitted, Reason reason) {
        static RoleChangeDecision allowed() { return new RoleChangeDecision(true, null); }
        static RoleChangeDecision denied(Reason reason) { return new RoleChangeDecision(false, reason); }
    }
    public enum Reason { ROLE_NOT_PERMITTED, NOT_RESOURCE_OWNER, SELF_GRANT_NOT_HELD, LAST_ADMINISTRATOR }
}
