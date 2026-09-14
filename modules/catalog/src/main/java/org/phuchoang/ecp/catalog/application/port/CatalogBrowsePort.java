package org.phuchoang.ecp.catalog.application.port;

import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Application-owned persistence port for catalog browse reads. Implementations return internal
 * projections so the application service, rather than persistence, owns cache and not-found
 * policy.
 */
public interface CatalogBrowsePort {
    /**
     * Tests whether a category exists regardless of whether it has products.
     *
     * @param id category identifier
     * @return {@code true} when a category row exists
     */
    boolean categoryExists(UUID id);
    /**
     * Tests whether a product exists and is published for guest-facing reads.
     *
     * @param id product identifier
     * @return {@code true} when the product is visible to browse callers
     */
    boolean publishedProductExists(UUID id);
    /**
     * Loads one category and its breadcrumb chain.
     *
     * @param id category identifier
     * @return category model, or empty when absent
     */
    Optional<CatalogBrowseModel.Category> category(UUID id);
    /**
     * Loads the complete category tree.
     *
     * @return recursively nested root categories
     */
    List<CatalogBrowseModel.CategoryNode> wholeTree();
    /**
     * Loads a complete tree or a depth-bounded subtree.
     *
     * @param rootId optional subtree root; {@code null} loads all roots
     * @param maxDepth optional number of descendant levels to include
     * @return recursively nested category nodes
     */
    List<CatalogBrowseModel.CategoryNode> tree(UUID rootId, Integer maxDepth);
    /**
     * Loads published product summaries within a category's materialized-path subtree.
     *
     * @param categoryId category that bounds the listing
     * @param query normalized listing query
     * @return resolved cursor page
     */
    CatalogBrowseModel.ProductPage products(UUID categoryId, CatalogBrowseModel.ListingQuery query);
    /**
     * Loads active and inactive variants matching every selected option.
     *
     * @param productId owning product identifier
     * @param selected selected option dimension/value pairs
     * @return variants compatible with the partial selection
     */
    List<CatalogBrowseModel.Variant> variants(UUID productId, Map<String, String> selected);
    /**
     * Loads one variant constrained to its owning product.
     *
     * @param productId owning product identifier
     * @param variantId variant identifier
     * @return variant model, or empty when the pair does not exist
     */
    Optional<CatalogBrowseModel.Variant> variant(UUID productId, UUID variantId);
}
