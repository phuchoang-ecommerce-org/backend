package org.phuchoang.ecp.catalog.internal.domain.policy;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.domain.service.CategoryHierarchyPolicy;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryHierarchyPolicyTest {
    private final CategoryHierarchyPolicy policy = new CategoryHierarchyPolicy();

    @Test
    void rejectsAMoveBelowADescendantAndReportsAStableReason() {
        UUID root = UUID.randomUUID();
        var decision = policy.planMove(new CategoryHierarchyPolicy.CategoryMoveContext(root, "/root/", UUID.randomUUID(),
            "/root/child/", 1), new CategoryHierarchyPolicy.CategorySubtreeSnapshot(List.of()));
        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).isEqualTo(CategoryHierarchyPolicy.Reason.DESCENDANT_PARENT);
    }

    @Test
    void rejectsDeletionWhenProductsRemainAssigned() {
        var decision = policy.validateDeletion(new CategoryHierarchyPolicy.CategoryOccupancy(0, 1));
        assertThat(decision.accepted()).isFalse();
        assertThat(decision.reason()).isEqualTo(CategoryHierarchyPolicy.Reason.HAS_PRODUCTS);
    }
}
