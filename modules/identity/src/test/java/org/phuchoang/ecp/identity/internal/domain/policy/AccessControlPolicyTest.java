package org.phuchoang.ecp.identity.internal.domain.policy;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.internal.domain.model.RoleCode;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlPolicyTest {
    private final AccessControlPolicy policy = new AccessControlPolicy();

    @Test
    void deniesAnAllowedRoleWhenTheActorDoesNotOwnTheResource() {
        var decision = policy.authorize(new AccessControlPolicy.AccessContext(UUID.randomUUID(), Set.of(RoleCode.CUSTOMER),
            "getOwnAccount", Set.of(RoleCode.CUSTOMER), UUID.randomUUID()));
        assertThat(decision.permitted()).isFalse();
        assertThat(decision.reason()).isEqualTo(AccessControlPolicy.Reason.NOT_RESOURCE_OWNER);
    }

    @Test
    void preventsSelfGrantAndRevokingTheLastAdministrator() {
        UUID administrator = UUID.randomUUID();
        var selfGrant = policy.evaluateRoleChange(new AccessControlPolicy.RoleChangeContext(administrator,
            Set.of(RoleCode.CUSTOMER), administrator, Set.of(RoleCode.CUSTOMER), RoleCode.ADMINISTRATOR, null, 1));
        var lastAdmin = policy.evaluateRoleChange(new AccessControlPolicy.RoleChangeContext(administrator,
            Set.of(RoleCode.ADMINISTRATOR), administrator, Set.of(RoleCode.ADMINISTRATOR), null, RoleCode.ADMINISTRATOR, 1));
        assertThat(selfGrant.reason()).isEqualTo(AccessControlPolicy.Reason.SELF_GRANT_NOT_HELD);
        assertThat(lastAdmin.reason()).isEqualTo(AccessControlPolicy.Reason.LAST_ADMINISTRATOR);
    }
}
