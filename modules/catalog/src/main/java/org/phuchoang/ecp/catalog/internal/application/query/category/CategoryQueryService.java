package org.phuchoang.ecp.catalog.internal.application.query.category;

import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheKeys;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCachePolicy;
import org.phuchoang.ecp.catalog.internal.application.port.CategoryBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogReadErrors;
import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryDetail;
import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryNode;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
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
        if (rootId == null && maxDepth == null) {
            CategoryTree cached = cache.getOrLoad(CatalogCacheKeys.categoryTree(), CatalogCachePolicy.BROWSE_TTL,
                () -> new CategoryTree(categories.wholeTree()), CategoryTree.class);
            return cached.items();
        }
        if (rootId != null && !categories.categoryExists(rootId)) {
            throw CatalogReadErrors.categoryNotFound();
        }
        return categories.tree(rootId, maxDepth);
    }

    public CategoryDetail getCategory(UUID categoryId) {
        return categories.category(categoryId).orElseThrow(CatalogReadErrors::categoryNotFound);
    }

    /** Cache serialization wrapper for the full navigation tree. */
    private record CategoryTree(List<CategoryNode> items) {
    }
}
