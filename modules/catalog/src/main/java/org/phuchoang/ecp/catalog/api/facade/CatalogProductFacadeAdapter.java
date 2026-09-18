package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.internal.application.query.product.ProductQueryService;
import org.phuchoang.ecp.catalog.api.view.category.CategoryRefView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.AdvisoryAvailabilityView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.RatingSummaryView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Maps catalog's internal product-detail read model to its named public interface. */
@Component
public class CatalogProductFacadeAdapter implements CatalogProductFacade {
    private final ProductQueryService service;
    private final CatalogDtoMapper mapper;

    public CatalogProductFacadeAdapter(ProductQueryService service, CatalogDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public ProductDetailView getProduct(UUID productId) {
        return mapper.productDetailView(service.getProduct(productId));
    }

    @Override
    public RatingSummaryView getProductRatingSummary(UUID productId) {
        return mapper.ratingSummaryView(service.getProductRatingSummary(productId));
    }
}
