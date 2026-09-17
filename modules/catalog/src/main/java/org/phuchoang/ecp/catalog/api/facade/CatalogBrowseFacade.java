package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.api.view.category.CategoryNodeView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.product.ProductPageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.catalog.api.query.CatalogListingQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public read surface for Sprint 06 catalog browsing. All cache keys are owned here so the
 * backend invalidation contract and the storefront's revalidation tags cannot silently drift.
 */
public interface CatalogBrowseFacade {

    /** Shared namespace for the complete category tree. */
    String CATEGORY_TREE_CACHE_KEY = "category-tree";
    /** Prefix for category-listing cache entries; implementations append an identifier and query fingerprint. */
    String CATEGORY_LISTING_CACHE_KEY_PREFIX = "category-listing:";
    /** Prefix for a single variant cache entry; implementations append the variant identifier. */
    String VARIANT_CACHE_KEY_PREFIX = "variant:";

    /**
     * Returns a category tree or the requested subtree.
     *
     * @param rootId optional root category; {@code null} requests the complete tree
     * @param maxDepth optional number of levels to include below the selected root
     * @return recursively nested categories in display order
     */
    List<CategoryNodeView> listCategories(UUID rootId, Integer maxDepth);

    /**
     * Gets a category and its root-to-parent breadcrumb chain.
     *
     * @param categoryId identifier of the category to retrieve
     * @return the category view
     * @throws org.phuchoang.ecp.sharedkernel.api.error.DomainException when the category is absent
     */
    CategoryView getCategory(UUID categoryId);

    /**
     * Lists published products in a category and all of its descendants.
     *
     * @param categoryId category that bounds the listing
     * @param query pagination, sort, and optional filter parameters
     * @return the requested page; a request past the final page resolves to the final page
     * @throws org.phuchoang.ecp.sharedkernel.api.error.DomainException when the category is absent
     */
    ProductPageView listCategoryProducts(UUID categoryId, CatalogListingQuery query);

    /**
     * Returns variants compatible with a complete or partial option selection.
     *
     * @param productId published product that owns the variants
     * @param options selected option dimension/value pairs; an empty map returns all variants
     * @return matching variants, including out-of-stock variants when availability is known
     * @throws org.phuchoang.ecp.sharedkernel.api.error.DomainException when the product is absent or unpublished
     */
    List<VariantView> listProductVariants(UUID productId, Map<String, String> options);

    /**
     * Gets one variant belonging to a published product.
     *
     * @param productId published product that owns the variant
     * @param variantId identifier of the requested variant
     * @return the variant view
     * @throws org.phuchoang.ecp.sharedkernel.api.error.DomainException when the product or variant is absent
     */
    VariantView getProductVariant(UUID productId, UUID variantId);

}
