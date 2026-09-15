package org.phuchoang.ecp.catalog.infrastructure.persistence.browse;

import org.phuchoang.ecp.catalog.application.port.CatalogBrowsePort;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Category;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.CategoryNode;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ListingQuery;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ProductPage;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ProductDetail;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Variant;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL read adapter. Reads only the catalog-owned tables; inventory
 * remains advisory until its sprint.
 */
@Repository
public class CatalogBrowseRepository implements CatalogBrowsePort {

  private final CatalogCategoryQueries categories;
  private final CatalogProductQueries products;
  private final CatalogProductListingQueries listings;

  CatalogBrowseRepository(CatalogCategoryQueries categories, CatalogProductQueries products,
      CatalogProductListingQueries listings) {
    this.categories = categories;
    this.products = products;
    this.listings = listings;
  }

  /** {@inheritDoc} */
  @Override
  public boolean categoryExists(UUID id) {
    return categories.exists(id);
  }

  /** {@inheritDoc} */
  @Override
  public boolean publishedProductExists(UUID id) {
    return products.publishedExists(id);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<ProductDetail> product(UUID id) {
    return products.product(id);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<Category> category(UUID id) {
    return categories.category(id);
  }

  /** {@inheritDoc} */
  @Override
  public List<CategoryNode> wholeTree() {
    return categories.wholeTree();
  }

  /** {@inheritDoc} */
  @Override
  public List<CategoryNode> tree(UUID rootId, Integer maxDepth) {
    return categories.tree(rootId, maxDepth);
  }

  /** {@inheritDoc} */
  @Override
  public ProductPage products(UUID categoryId, ListingQuery query) {
    return listings.products(categoryId, query);
  }

  /** {@inheritDoc} */
  @Override
  public List<Variant> variants(UUID productId, Map<String, String> selected) {
    return products.variants(productId, selected);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<Variant> variant(UUID productId, UUID variantId) {
    return products.variant(productId, variantId);
  }
}
