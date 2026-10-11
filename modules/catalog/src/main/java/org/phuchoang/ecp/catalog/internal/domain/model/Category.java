package org.phuchoang.ecp.catalog.internal.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Category hierarchy aggregate and owner of category-change facts. */
@AggregateRoot
@Getter
@EqualsAndHashCode
public final class Category implements CatalogEventSource {
    @Identity private UUID id;
    private UUID parentId;
    private String name;
    private String slug;
    private String path;
    private int depth;
    private String imageUrl;
    private int sortOrder;
    private boolean featured;
    @EqualsAndHashCode.Exclude private final List<CatalogDomainEvent> domainEvents = new ArrayList<>();

    /** Reconstitutes a category without raising an event. */
    public Category(UUID id, UUID parentId, String name, String slug, String path, int depth, String imageUrl,
            int sortOrder, boolean featured) {
        if (id == null || path == null || !path.startsWith("/") || !path.endsWith("/")) {
            throw new IllegalArgumentException("Category requires a slash-delimited materialised path.");
        }
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.slug = slug;
        this.path = path;
        this.depth = depth;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.featured = featured;
    }

    public static Category create(UUID id, UUID parentId, String name, String slug, String imageUrl, int sortOrder,
            boolean featured, Category parent) {
        String path = parent == null ? "/" + id + "/" : parent.path + id + "/";
        int depth = parent == null ? 0 : parent.depth + 1;
        Category category = new Category(id, parentId, name, slug, path, depth, imageUrl, sortOrder, featured);
        category.domainEvents.add(new CategoryChanged(category.snapshot(), false));
        return category;
    }

    /** Loads this aggregate through its domain persistence boundary. */
    public static Optional<Category> findById(UUID id, CategoryRepository repository) {
        return repository.findById(id);
    }

    public Category change(UUID updatedParentId, String updatedName, String updatedImageUrl, int updatedSortOrder,
            boolean updatedFeatured, Category parent) {
        if (!mayMoveBelow(parent)) {
            throw new CategoryHierarchyViolation("A category cannot be moved beneath itself or a descendant.");
        }
        parentId = updatedParentId;
        name = updatedName;
        path = parent == null ? "/" + id + "/" : parent.path + id + "/";
        depth = parent == null ? 0 : parent.depth + 1;
        imageUrl = updatedImageUrl;
        sortOrder = updatedSortOrder;
        featured = updatedFeatured;
        domainEvents.add(new CategoryChanged(snapshot(), false));
        return this;
    }

    public boolean mayMoveBelow(Category proposedParent) {
        return proposedParent == null || !proposedParent.path.startsWith(path);
    }

    /** Records removal before deleting this aggregate through its persistence boundary. */
    public void remove() { domainEvents.add(new CategoryChanged(snapshot(), true)); }

    public void persist(CategoryRepository repository) {
        repository.save(this);
    }

    public long childCategoryCount(CategoryRepository repository) {
        return repository.countByParentId(id);
    }

    public long assignedProductCount(ProductRepository repository) {
        return repository.countByCategoryId(id);
    }

    public void remove(CategoryRepository repository) {
        remove();
        repository.deleteById(id);
    }

    public UUID id() { return id; }
    public UUID parentId() { return parentId; }
    public String name() { return name; }
    public String slug() { return slug; }
    public String path() { return path; }
    public int depth() { return depth; }
    public String imageUrl() { return imageUrl; }
    public int sortOrder() { return sortOrder; }
    public boolean featured() { return featured; }

    @Override
    public List<CatalogDomainEvent> pullDomainEvents() {
        List<CatalogDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private Category snapshot() {
        return new Category(id, parentId, name, slug, path, depth, imageUrl, sortOrder, featured);
    }
}
