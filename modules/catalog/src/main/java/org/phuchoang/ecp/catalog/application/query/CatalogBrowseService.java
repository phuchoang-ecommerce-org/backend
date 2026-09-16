package org.phuchoang.ecp.catalog.application.query;

import org.phuchoang.ecp.catalog.application.port.CatalogBrowsePort;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sprint 06 browse use cases. A Redis failure intentionally follows the same
 * database path as a miss.
 */
@Service
public class CatalogBrowseService {

  /** Cache lifetime for all current catalog browse surfaces. */
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  /** Browse read port backed by the catalog-owned persistence adapter. */
  private final CatalogBrowsePort repository;
  /** Cache-aside abstraction that treats a cache failure like a cache miss. */
  private final CacheAside cacheAside;

  /**
   * Creates the catalog browse use-case service.
   *
   * @param repository source of catalog read projections
   * @param cacheAside resilient cache-aside coordinator
   */
  public CatalogBrowseService(CatalogBrowsePort repository, CacheAside cacheAside) {
    this.repository = repository;
    this.cacheAside = cacheAside;
  }

  /**
   * Lists the whole category tree from the shared cache or loads a requested subtree directly.
   *
   * @param rootId optional subtree root; {@code null} selects the cacheable full tree
   * @param maxDepth optional descendant depth limit
   * @return recursive category navigation nodes
   * @throws DomainException when a supplied root category is absent
   */
  public List<CatalogBrowseModel.CategoryNode> listCategories(UUID rootId, Integer maxDepth) {
    if (rootId == null && maxDepth == null) {
      CategoryTree cached = cacheAside.getOrLoad("category-tree", CACHE_TTL,
          () -> new CategoryTree(repository.wholeTree()), CategoryTree.class);
      return cached.items();
    }
    if (rootId != null && !repository.categoryExists(rootId)) {
      throw notFound("Category not found.");
    }
    return repository.tree(rootId, maxDepth);
  }

  /**
   * Gets one category and its breadcrumb chain.
   *
   * @param categoryId category identifier
   * @return category projection
   * @throws DomainException when the category is absent
   */
  public CatalogBrowseModel.Category getCategory(UUID categoryId) {
    return repository.category(categoryId).orElseThrow(() -> notFound("Category not found."));
  }

  /**
   * Lists published products in a category subtree through a query-specific cache entry.
   *
   * @param categoryId category that bounds the listing
   * @param query normalized pagination, sort, and filter inputs
   * @return resolved listing page
   * @throws DomainException when the category is absent
   */
  public CatalogBrowseModel.ProductPage listCategoryProducts(UUID categoryId, CatalogBrowseModel.ListingQuery query) {
    if (!repository.categoryExists(categoryId)) {
      throw notFound("Category not found.");
    }
    String key = "category-listing:" + categoryId + ":" + listingFingerprint(query);
    return cacheAside.getOrLoad(key, CACHE_TTL, () -> repository.products(categoryId, query),
        CatalogBrowseModel.ProductPage.class);
  }

  /**
   * Returns all variants consistent with a partial option selection.
   *
   * @param productId published product identifier
   * @param options selected option dimension/value pairs
   * @return compatible variants
   * @throws DomainException when the product is absent or unpublished
   */
  public List<CatalogBrowseModel.Variant> listProductVariants(UUID productId, Map<String, String> options) {
    if (!repository.publishedProductExists(productId)) {
      throw notFound("Product not found.");
    }
    return repository.variants(productId, options);
  }

  /**
   * Returns a variant from its single-variant cache entry.
   *
   * @param productId published product identifier
   * @param variantId variant identifier
   * @return variant projection
   * @throws DomainException when the product or variant is absent
   */
  public CatalogBrowseModel.Variant getProductVariant(UUID productId, UUID variantId) {
    if (!repository.publishedProductExists(productId)) {
      throw notFound("Product not found.");
    }
    String key = "variant:" + variantId;
    return cacheAside.getOrLoad(key, CACHE_TTL,
        () -> repository.variant(productId, variantId).orElseThrow(() -> notFound("Variant not found.")),
        CatalogBrowseModel.Variant.class);
  }

  /**
   * Produces a stable cache-key suffix for every listing input that affects a result.
   *
   * @param query normalized listing inputs
   * @return URL-safe Base64 representation of the input tuple
   */
  private static String listingFingerprint(CatalogBrowseModel.ListingQuery query) {
    String raw = String.join("|", String.valueOf(query.cursor()), String.valueOf(query.size()),
        String.valueOf(query.sort()), String.valueOf(query.brands()), String.valueOf(query.priceFrom()),
        String.valueOf(query.priceTo()), String.valueOf(query.inStock()));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Creates the catalog's uniform not-found error.
   *
   * @param detail externally safe problem detail
   * @return not-found domain exception
   */
  private static DomainException notFound(String detail) {
    return new DomainException(GenErrorCode.NOT_FOUND, detail);
  }

  /**
   * Cache serialization wrapper for the full navigation tree.
   *
   * @param items root category nodes
   */
  private record CategoryTree(List<CatalogBrowseModel.CategoryNode> items) {
  }
}
