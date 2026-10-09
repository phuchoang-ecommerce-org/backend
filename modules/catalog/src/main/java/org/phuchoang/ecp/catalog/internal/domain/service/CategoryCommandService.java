package org.phuchoang.ecp.catalog.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Authoritative category write boundary. It enforces the cross-aggregate
 * empty-category rule
 * before removal and persists category changes only through aggregate
 * transitions.
 */
@Service
public final class CategoryCommandService {

  private final CategoryRepository categories;
  private final ProductRepository products;
  private final CategoryHierarchyPolicy hierarchyPolicy;

  public CategoryCommandService(CategoryRepository categories, ProductRepository products,
      CategoryHierarchyPolicy hierarchyPolicy) {
    this.categories = categories;
    this.products = products;
    this.hierarchyPolicy = hierarchyPolicy;
  }

  public Optional<Category> find(UUID categoryId) {
    return categories.findById(categoryId);
  }

  public Category create(UUID id, UUID parentId, String name, String slug, String imageUrl, int sortOrder,
      boolean featured, Category parent) {
    return categories.save(Category.create(id, parentId, name, slug, imageUrl, sortOrder, featured, parent));
  }

  public Category change(Category category, UUID parentId, String name, String imageUrl, int sortOrder,
      boolean featured, Category parent) {
    return categories.save(category.change(parentId, name, imageUrl, sortOrder, featured, parent));
  }

  public DeletionDecision delete(UUID categoryId) {
    Category category = categories.findById(categoryId).orElse(null);
    if (category == null) {
      return DeletionDecision.missingDecision();
    }
    long childCategoryCount = categories.countByParentId(categoryId);
    long assignedProductCount = products.countByCategoryId(categoryId);
    var decision = hierarchyPolicy.validateDeletion(new CategoryHierarchyPolicy.CategoryOccupancy(
        childCategoryCount, assignedProductCount));
    if (!decision.accepted()) {
      return DeletionDecision.rejected(childCategoryCount, assignedProductCount);
    }
    categories.deleteById(categoryId);
    return DeletionDecision.deleted(category);
  }

  public record DeletionDecision(Status status, Category category, long childCategoryCount, long assignedProductCount) {

    private static DeletionDecision missingDecision() {
      return new DeletionDecision(Status.MISSING, null, 0, 0);
    }

    private static DeletionDecision rejected(long childCategoryCount, long assignedProductCount) {
      return new DeletionDecision(Status.REJECTED, null, childCategoryCount, assignedProductCount);
    }

    private static DeletionDecision deleted(Category category) {
      return new DeletionDecision(Status.DELETED, category, 0, 0);
    }

    public boolean missing() {
      return status == Status.MISSING;
    }

    public boolean accepted() {
      return status == Status.DELETED;
    }

    public enum Status {
      MISSING, REJECTED, DELETED
    }
  }
}
