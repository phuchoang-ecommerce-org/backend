package org.phuchoang.ecp.catalog.internal.domain.model;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void ownsItsRepositoryPersistenceAndOccupancyOperations() {
        CategoryRepository categories = mock(CategoryRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        Category category = new Category(UUID.randomUUID(), null, "Root", "root", "/root/", 0, null, 0, false);
        when(categories.save(category)).thenReturn(category);
        when(categories.countByParentId(category.id())).thenReturn(3L);
        when(products.countByCategoryId(category.id())).thenReturn(5L);

        category.create(categories);
        assertThat(category.childCategoryCount(categories)).isEqualTo(3L);
        assertThat(category.assignedProductCount(products)).isEqualTo(5L);
        category.remove(categories);

        verify(categories).save(category);
        verify(categories).deleteById(category.id());
    }
}
