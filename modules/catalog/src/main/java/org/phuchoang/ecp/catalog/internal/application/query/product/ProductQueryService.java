package org.phuchoang.ecp.catalog.internal.application.query.product;

import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCacheKeys;
import org.phuchoang.ecp.catalog.internal.application.cache.CatalogCachePolicy;
import org.phuchoang.ecp.catalog.internal.application.port.CategoryBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.port.ProductDetailPort;
import org.phuchoang.ecp.catalog.internal.application.port.ProductListingPort;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogReadErrors;
import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductDetail;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.RatingSummary;
import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** Product detail and category-listing reads, each behind its own cache entry. */
@Service
public class ProductQueryService {

    private final ProductDetailPort products;
    private final ProductListingPort listings;
    private final CategoryBrowsePort categories;
    private final CacheAside cache;

    public ProductQueryService(ProductDetailPort products, ProductListingPort listings, CategoryBrowsePort categories,
            CacheAside cache) {
        this.products = products;
        this.listings = listings;
        this.categories = categories;
        this.cache = cache;
    }

    public ProductDetail getProduct(UUID productId) {
        return cache.getOrLoad(CatalogCacheKeys.product(productId), CatalogCachePolicy.PRODUCT_DETAIL_TTL,
            () -> products.product(productId).orElseThrow(CatalogReadErrors::productNotFound), ProductDetail.class);
    }

    public RatingSummary getProductRatingSummary(UUID productId) {
        if (!products.publishedProductExists(productId)) {
            throw CatalogReadErrors.productNotFound();
        }
        return RatingSummary.EMPTY;
    }

    /**
     * Lists published products in a category subtree through a query-specific cache entry. The
     * existence check stays: the API distinguishes an empty listing from an unknown category.
     */
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
