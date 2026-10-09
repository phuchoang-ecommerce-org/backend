package org.phuchoang.ecp.catalog.internal.application.browse.query.category;

import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheKeys;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCachePolicy;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.browse.query.CatalogReadErrors;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryDetail;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryNode;
import org.phuchoang.ecp.catalog.internal.application.cache.CacheAside;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Category navigation reads. A Redis failure intentionally follows the same database path as a miss. */
@Service
public class CategoryQueryService {

    private final CategoryBrowsePort categories;
    private final CacheAside cache;

    public CategoryQueryService(CategoryBrowsePort categories, CacheAside cache) {
        this.categories = categories;
        this.cache = cache;
    }

    /**
     * Lists the whole category tree from the shared cache, or loads a requested subtree directly.
     *
     * @param rootId optional subtree root; {@code null} selects the cacheable full tree
     * @param maxDepth optional descendant depth limit
     */
    public List<CategoryNode> listCategories(UUID rootId, Integer maxDepth) {
        if (rootId != null && !categories.categoryExists(rootId)) {
            throw CatalogReadErrors.categoryNotFound();
        }
        CategoryTree cached = cache.getOrLoad(CatalogCacheKeys.categoryTree(rootId, maxDepth), CatalogCachePolicy.BROWSE_TTL,
            () -> new CategoryTree(rootId == null && maxDepth == null ? categories.wholeTree() : categories.tree(rootId, maxDepth)),
            CategoryTree.class);
        return cached.items();
    }

    public CategoryDetail getCategory(UUID categoryId) {
        return categories.category(categoryId).orElseThrow(CatalogReadErrors::categoryNotFound);
    }

    /** Cache serialization wrapper for the full navigation tree. */
    private record CategoryTree(List<CategoryNode> items) {
    }
}
