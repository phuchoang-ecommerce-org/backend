package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseService;
import org.phuchoang.ecp.catalog.api.view.category.CategoryNodeView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryRefView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.AdvisoryAvailabilityView;
import org.phuchoang.ecp.catalog.api.view.product.ProductPageView;
import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.catalog.api.query.CatalogListingQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maps the application-owned catalog read model to the public named-interface contract. This is
 * the only adapter through which the web composition root reaches browse behavior.
 */
@Component
public class CatalogBrowseFacadeAdapter implements CatalogBrowseFacade {
    /** Internal browse-use-case service. */
    private final CatalogBrowseService service;
    private final CatalogDtoMapper mapper;

    /**
     * Creates the public-contract adapter.
     *
     * @param service browse-use-case implementation to delegate to
     */
    public CatalogBrowseFacadeAdapter(CatalogBrowseService service, CatalogDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    /** {@inheritDoc} */
    public List<CategoryNodeView> listCategories(UUID rootId, Integer maxDepth) {
        return service.listCategories(rootId, maxDepth).stream().map(mapper::categoryNodeView).toList();
    }

    @Override
    /** {@inheritDoc} */
    public CategoryView getCategory(UUID categoryId) {
        return mapper.categoryView(service.getCategory(categoryId));
    }

    @Override
    /** {@inheritDoc} */
    public ProductPageView listCategoryProducts(UUID categoryId, CatalogListingQuery query) {
        return mapper.productPageView(service.listCategoryProducts(categoryId, mapper.listingQuery(query)));
    }

    @Override
    /** {@inheritDoc} */
    public List<VariantView> listProductVariants(UUID productId, Map<String, String> options) {
        return service.listProductVariants(productId, options).stream().map(mapper::variantView).toList();
    }

    @Override
    /** {@inheritDoc} */
    public VariantView getProductVariant(UUID productId, UUID variantId) {
        return mapper.variantView(service.getProductVariant(productId, variantId));
    }

}
