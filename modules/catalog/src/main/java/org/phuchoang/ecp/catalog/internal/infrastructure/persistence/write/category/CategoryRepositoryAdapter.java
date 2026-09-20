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
    private final CategoryJpaMapper mapper;

    CategoryRepositoryAdapter(CatalogCategoryJpaRepository categories, JdbcCategoryHierarchyStore hierarchy, Clock clock,
            CategoryJpaMapper mapper) {
        this.categories = categories;
        this.hierarchy = hierarchy;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categories.findById(id).map(mapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        CatalogCategoryEntity entity = categories.findById(category.id()).orElse(null);
        if (entity == null) {
            entity = mapper.toEntity(category);
            entity.initializeAuditTimestamps(Instant.now(clock));
            return mapper.toDomain(categories.save(entity));
        }
        if (!entity.path().equals(category.path())) {
            hierarchy.relocateSubtree(entity.path(), entity.depth(), category.path(), category.depth());
            entity = categories.findById(category.id()).orElseThrow();
        }
        entity.update(category.parentId(), category.name(), category.path(), category.depth(), category.imageUrl(),
            category.sortOrder(), category.featured(), Instant.now(clock));
        return mapper.toDomain(categories.save(entity));
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

}
