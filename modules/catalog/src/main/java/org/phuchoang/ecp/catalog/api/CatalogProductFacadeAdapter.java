package org.phuchoang.ecp.catalog.api;

import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel;
import org.phuchoang.ecp.catalog.application.query.CatalogProductService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Maps catalog's internal product-detail read model to its named public interface. */
@Component
public class CatalogProductFacadeAdapter implements CatalogProductFacade {
    private final CatalogProductService service;

    public CatalogProductFacadeAdapter(CatalogProductService service) {
        this.service = service;
    }

    @Override
    public ProductDetailView getProduct(UUID productId) {
        return detail(service.getProduct(productId));
    }

    @Override
    public RatingSummaryView getProductRatingSummary(UUID productId) {
        CatalogBrowseModel.RatingSummary summary = service.getProductRatingSummary(productId);
        return new RatingSummaryView(summary.averageRating(), summary.reviewCount(), summary.distribution());
    }

    private ProductDetailView detail(CatalogBrowseModel.ProductDetail value) {
        return new ProductDetailView(value.id(), value.name(), value.slug(), value.description(), value.brand(),
            value.publicationStatus(), value.publishedAt(),
            value.categories().stream().map(category -> new CategoryRefView(category.id(), category.name(), category.slug())).toList(),
            value.attributes(), value.images().stream().map(image -> new ProductImageView(image.id(), image.url(), image.altText(), image.sortOrder())).toList(),
            value.variants().stream().map(this::variant).toList(), value.averageRating(), value.reviewCount());
    }

    private VariantView variant(CatalogBrowseModel.Variant value) {
        return new VariantView(value.id(), value.sku(), value.name(), money(value.listPrice()), money(value.promotionalPrice()),
            value.options(), value.weightGrams(), value.active(),
            value.inStock() == null ? null : new AdvisoryAvailabilityView(value.inStock()));
    }

    private MoneyView money(CatalogBrowseModel.Money value) {
        return value == null ? null : new MoneyView(value.amount(), value.currency());
    }
}
