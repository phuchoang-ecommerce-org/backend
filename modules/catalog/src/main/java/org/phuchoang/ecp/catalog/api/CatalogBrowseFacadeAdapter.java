package org.phuchoang.ecp.catalog.api;

import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseService;
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

    /**
     * Creates the public-contract adapter.
     *
     * @param service browse-use-case implementation to delegate to
     */
    public CatalogBrowseFacadeAdapter(CatalogBrowseService service) {
        this.service = service;
    }

    @Override
    /** {@inheritDoc} */
    public List<CategoryNodeView> listCategories(UUID rootId, Integer maxDepth) {
        return service.listCategories(rootId, maxDepth).stream().map(this::node).toList();
    }

    @Override
    /** {@inheritDoc} */
    public CategoryView getCategory(UUID categoryId) {
        return category(service.getCategory(categoryId));
    }

    @Override
    /** {@inheritDoc} */
    public ProductPageView listCategoryProducts(UUID categoryId, CatalogListingQuery query) {
        CatalogBrowseModel.ProductPage page = service.listCategoryProducts(categoryId,
            new CatalogBrowseModel.ListingQuery(query.cursor(), query.size(), query.sort(), query.brands(),
                query.priceFrom(), query.priceTo(), query.inStock()));
        return new ProductPageView(page.items().stream().map(this::product).toList(), page.nextCursor(), page.total());
    }

    @Override
    /** {@inheritDoc} */
    public List<VariantView> listProductVariants(UUID productId, Map<String, String> options) {
        return service.listProductVariants(productId, options).stream().map(this::variant).toList();
    }

    @Override
    /** {@inheritDoc} */
    public VariantView getProductVariant(UUID productId, UUID variantId) {
        return variant(service.getProductVariant(productId, variantId));
    }

    /**
     * Converts an internal category to a public direct-lookup view.
     *
     * @param value internal category model
     * @return public category view
     */
    private CategoryView category(CatalogBrowseModel.Category value) {
        return new CategoryView(value.id(), value.parentId(), value.name(), value.slug(), value.depth(), value.sortOrder(),
            value.imageUrl(), value.featured(), value.ancestors().stream().map(this::ref).toList());
    }

    /**
     * Converts an internal tree node recursively.
     *
     * @param value internal category node
     * @return public category node with converted children
     */
    private CategoryNodeView node(CatalogBrowseModel.CategoryNode value) {
        return new CategoryNodeView(value.id(), value.parentId(), value.name(), value.slug(), value.depth(), value.sortOrder(),
            value.imageUrl(), value.featured(), value.ancestors().stream().map(this::ref).toList(),
            value.children().stream().map(this::node).toList());
    }

    /**
     * Converts an internal breadcrumb item.
     *
     * @param value internal category reference
     * @return public category reference
     */
    private CategoryRefView ref(CatalogBrowseModel.CategoryRef value) {
        return new CategoryRefView(value.id(), value.name(), value.slug());
    }

    /**
     * Converts an internal money value while preserving an absent price.
     *
     * @param value internal money value, or {@code null}
     * @return public money view, or {@code null}
     */
    private MoneyView money(CatalogBrowseModel.Money value) {
        return value == null ? null : new MoneyView(value.amount(), value.currency());
    }

    /**
     * Converts an internal listing projection.
     *
     * @param value internal product summary
     * @return public product summary
     */
    private ProductSummaryView product(CatalogBrowseModel.ProductSummary value) {
        return new ProductSummaryView(value.id(), value.name(), value.slug(), value.brand(), value.publicationStatus(),
            value.primaryImageUrl(), money(value.priceFrom()), money(value.priceTo()), value.averageRating(), value.reviewCount(),
            value.inStock());
    }

    /**
     * Converts an internal variant projection.
     *
     * @param value internal variant
     * @return public variant view
     */
    private VariantView variant(CatalogBrowseModel.Variant value) {
        return new VariantView(value.id(), value.sku(), value.name(), money(value.listPrice()),
            money(value.promotionalPrice()), value.options(), value.weightGrams(), value.active(),
            value.inStock() == null ? null : new AdvisoryAvailabilityView(value.inStock()));
    }
}
