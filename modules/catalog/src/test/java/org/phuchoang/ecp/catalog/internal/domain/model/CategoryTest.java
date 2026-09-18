package org.phuchoang.ecp.catalog.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void derivesPathAndDepthFromItsParent() {
        Category parent = new Category(UUID.randomUUID(), null, "Parent", "parent", "/parent/", 0, null, 0, false);
        UUID id = UUID.randomUUID();

        Category category = Category.create(id, parent.id(), "Child", "child", null, 2, true, parent);

        assertThat(category.path()).isEqualTo("/parent/" + id + "/");
        assertThat(category.depth()).isEqualTo(1);
    }

    @Test
    void refusesToMoveBeneathItselfOrADescendant() {
        Category category = new Category(UUID.randomUUID(), null, "Root", "root", "/root/", 0, null, 0, false);
        Category descendant = new Category(UUID.randomUUID(), category.id(), "Child", "child", "/root/child/", 1, null, 0, false);

        assertThat(category.mayMoveBelow(descendant)).isFalse();
        assertThatThrownBy(() -> category.change(descendant.id(), category.name(), null, 0, false, descendant))
            .isInstanceOf(CategoryHierarchyViolation.class);
    }
}
