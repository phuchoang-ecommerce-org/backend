package org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct;

import org.phuchoang.ecp.catalog.internal.application.browse.CatalogReadErrors;
import org.phuchoang.ecp.catalog.internal.application.browse.category.CategoryBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheKeys;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCachePolicy;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** Lists published products in a category subtree through a query-specific cache entry. */
@Service
public class CategoryProductListingService {

    private final ProductListingPort listings;
    private final CategoryBrowsePort categories;
    private final CacheAside cache;

    public CategoryProductListingService(ProductListingPort listings, CategoryBrowsePort categories, CacheAside cache) {
        this.listings = listings;
        this.categories = categories;
        this.cache = cache;
    }

    /** The existence check preserves the API distinction between an empty listing and an unknown category. */
    public ProductPage listCategoryProducts(UUID categoryId, ProductListingQuery query) {
        if (!categories.categoryExists(categoryId)) {
            throw CatalogReadErrors.categoryNotFound();
        }
        String key = CatalogCacheKeys.categoryListing(categoryId, fingerprint(query));
        return cache.getOrLoad(key, CatalogCachePolicy.BROWSE_TTL, () -> listings.products(categoryId, query),
            ProductPage.class);
    }

    /** A stable cache-key suffix for every listing input that affects a result. */
    static String fingerprint(ProductListingQuery query) {
        String raw = String.join("|", String.valueOf(query.cursor()), String.valueOf(query.size()),
            String.valueOf(query.sort()), String.valueOf(query.brands()), String.valueOf(query.priceFrom()),
            String.valueOf(query.priceTo()), String.valueOf(query.inStock()));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
