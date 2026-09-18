package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.category;

import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.SubtreeCategory;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter for the {@link Category} aggregate persistence port. */
@Repository
class CategoryRepositoryAdapter implements CategoryRepository {
    private final CatalogCategoryJpaRepository categories;
    private final JdbcCategoryHierarchyStore hierarchy;
    private final Clock clock;

    CategoryRepositoryAdapter(CatalogCategoryJpaRepository categories, JdbcCategoryHierarchyStore hierarchy, Clock clock) {
        this.categories = categories;
        this.hierarchy = hierarchy;
        this.clock = clock;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categories.findById(id).map(this::toDomain);
    }

    @Override
    public Category save(Category category) {
        CatalogCategoryEntity entity = categories.findById(category.id()).orElse(null);
        if (entity == null) {
            return toDomain(categories.save(new CatalogCategoryEntity(category.id(), category.parentId(), category.name(),
                category.slug(), category.path(), category.depth(), category.imageUrl(), category.sortOrder(), category.featured(),
                Instant.now(clock))));
        }
        if (!entity.path().equals(category.path())) {
            hierarchy.relocateSubtree(entity.path(), entity.depth(), category.path(), category.depth());
            entity = categories.findById(category.id()).orElseThrow();
        }
        entity.update(category.parentId(), category.name(), category.path(), category.depth(), category.imageUrl(),
            category.sortOrder(), category.featured(), Instant.now(clock));
        return toDomain(categories.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        categories.deleteById(id);
    }

    @Override
    public long countByParentId(UUID parentId) {
        return categories.countByParentId(parentId);
    }

    @Override
    public List<SubtreeCategory> findSubtree(UUID categoryId) {
        return hierarchy.findSubtree(categoryId);
    }

    private Category toDomain(CatalogCategoryEntity entity) {
        return new Category(entity.id(), entity.parentId(), entity.name(), entity.slug(), entity.path(), entity.depth(),
            entity.imageUrl(), entity.sortOrder(), entity.featured());
    }
}
