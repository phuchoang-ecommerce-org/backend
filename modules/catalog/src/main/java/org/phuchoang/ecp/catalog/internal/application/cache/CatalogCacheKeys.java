package org.phuchoang.ecp.catalog.internal.application.cache;

import java.util.UUID;

/**
 * The physical cache keys of every Catalog read surface, in one place so population and
 * invalidation can never disagree (Backend Architecture.md §5.7). Nothing outside this package
 * builds a key string.
 */
public final class CatalogCacheKeys {

    static final String CATEGORY_TREE = "category-tree";
    static final String CATEGORY_TREE_PREFIX = CATEGORY_TREE + ":";
    static final String CATEGORY_LISTING_PREFIX = "category-listing:";
    static final String VARIANT_PREFIX = "variant:";
    static final String PRODUCT_PREFIX = "cat:product:";

    private CatalogCacheKeys() {
    }

    public static String categoryTree() {
        return CATEGORY_TREE;
    }

    /** One entry per whole-tree or requested subtree/depth combination. */
    public static String categoryTree(UUID rootId, Integer maxDepth) {
        if (rootId == null && maxDepth == null) {
            return CATEGORY_TREE;
        }
        return CATEGORY_TREE_PREFIX + String.valueOf(rootId) + ":" + String.valueOf(maxDepth);
    }

    /** The full navigation tree and every scoped-tree variant share this invalidation unit. */
    public static String categoryTreePrefix() {
        return CATEGORY_TREE;
    }

    /** One entry per distinct listing query beneath a category. */
    public static String categoryListing(UUID categoryId, String queryFingerprint) {
        return categoryListingPrefix(categoryId) + queryFingerprint;
    }

    /** Every listing entry beneath {@code categoryId}, whatever its query — the invalidation unit. */
    public static String categoryListingPrefix(UUID categoryId) {
        return CATEGORY_LISTING_PREFIX + categoryId + ":";
    }

    public static String variant(UUID variantId) {
        return VARIANT_PREFIX + variantId;
    }

    public static String product(UUID productId) {
        return PRODUCT_PREFIX + productId;
    }
}
