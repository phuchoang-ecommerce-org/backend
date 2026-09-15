package org.phuchoang.ecp.catalog.application.query;

import org.phuchoang.ecp.catalog.api.CatalogProductFacade;
import org.phuchoang.ecp.catalog.application.port.CatalogBrowsePort;
import org.phuchoang.ecp.sharedkernel.api.CacheAside;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/** Product-detail reads, independently cacheable from category browsing. */
@Service
public class CatalogProductService {
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofMinutes(15);

    private final CatalogBrowsePort repository;
    private final CacheAside cacheAside;

    public CatalogProductService(CatalogBrowsePort repository, CacheAside cacheAside) {
        this.repository = repository;
        this.cacheAside = cacheAside;
    }

    public CatalogBrowseModel.ProductDetail getProduct(UUID productId) {
        return cacheAside.getOrLoad(CatalogProductFacade.PRODUCT_CACHE_KEY_PREFIX + productId, PRODUCT_CACHE_TTL,
            () -> repository.product(productId).orElseThrow(CatalogProductService::notFound),
            CatalogBrowseModel.ProductDetail.class);
    }

    public CatalogBrowseModel.RatingSummary getProductRatingSummary(UUID productId) {
        if (!repository.publishedProductExists(productId)) {
            throw notFound();
        }
        return CatalogBrowseModel.RatingSummary.EMPTY;
    }

    private static DomainException notFound() {
        return new DomainException(GenErrorCode.NOT_FOUND, "Product not found.");
    }
}
