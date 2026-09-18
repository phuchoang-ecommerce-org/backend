package org.phuchoang.ecp.catalog.api.facade;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.api.query.CatalogListingQuery;
import org.phuchoang.ecp.catalog.api.request.BulkItem;
import org.phuchoang.ecp.catalog.api.request.CategoryWrite;
import org.phuchoang.ecp.catalog.api.request.ImageWrite;
import org.phuchoang.ecp.catalog.api.request.PriceWrite;
import org.phuchoang.ecp.catalog.api.request.ProductWrite;
import org.phuchoang.ecp.catalog.api.request.PublicationWrite;
import org.phuchoang.ecp.catalog.api.request.VariantWrite;
import org.phuchoang.ecp.catalog.api.result.BulkItemResult;
import org.phuchoang.ecp.catalog.api.result.BulkResult;
import org.phuchoang.ecp.catalog.api.view.category.CategoryNodeView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryRefView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.AdvisoryAvailabilityView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.ProductPageView;
import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;
import org.phuchoang.ecp.catalog.api.view.product.RatingSummaryView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.catalog.internal.application.command.category.CategoryChange;
import org.phuchoang.ecp.catalog.internal.application.command.category.CategorySnapshot;
import org.phuchoang.ecp.catalog.internal.application.command.category.CreateCategory;
import org.phuchoang.ecp.catalog.internal.application.command.image.AddImage;
import org.phuchoang.ecp.catalog.internal.application.command.image.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.command.product.BulkAmendment;
import org.phuchoang.ecp.catalog.internal.application.command.product.BulkAmendmentOutcome;
import org.phuchoang.ecp.catalog.internal.application.command.product.CreateProduct;
import org.phuchoang.ecp.catalog.internal.application.command.product.ProductChange;
import org.phuchoang.ecp.catalog.internal.application.command.product.ProductSnapshot;
import org.phuchoang.ecp.catalog.internal.application.command.product.SetPublication;
import org.phuchoang.ecp.catalog.internal.application.command.variant.AddVariant;
import org.phuchoang.ecp.catalog.internal.application.command.variant.Price;
import org.phuchoang.ecp.catalog.internal.application.command.variant.VariantSnapshot;
import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryDetail;
import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryNode;
import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryRef;
import org.phuchoang.ecp.catalog.internal.application.query.model.common.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductDetail;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductImage;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductSummary;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.RatingSummary;
import org.phuchoang.ecp.catalog.internal.application.query.model.variant.VariantDetail;

import java.util.List;

/**
 * Maps public Catalog DTOs to application commands and application results to API views. The
 * request-side defaults (an unnamed variant is named after its SKU, variants are active, images
 * and categories sort first, categories are not featured) are deterministic API conventions and
 * therefore live here, not in the use cases.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CatalogDtoMapper {

    // ---- read side ----

    ProductListingQuery listingQuery(CatalogListingQuery query);

    CategoryView categoryView(CategoryDetail category);

    CategoryNodeView categoryNodeView(CategoryNode category);

    CategoryRefView categoryRefView(CategoryRef category);

    MoneyView moneyView(MoneyValue money);

    ProductSummaryView productSummaryView(ProductSummary product);

    ProductPageView productPageView(ProductPage page);

    @Mapping(target = "availability", source = "inStock")
    VariantView variantView(VariantDetail variant);

    ProductImageView productImageView(ProductImage image);

    ProductDetailView productDetailView(ProductDetail product);

    RatingSummaryView ratingSummaryView(RatingSummary rating);

    default AdvisoryAvailabilityView advisoryAvailabilityView(Boolean inStock) {
        return inStock == null ? null : new AdvisoryAvailabilityView(inStock);
    }

    // ---- command side: requests -> commands ----

    CreateProduct createProduct(ProductWrite request);

    ProductChange productChange(ProductWrite request);

    SetPublication setPublication(PublicationWrite request);

    default AddVariant addVariant(VariantWrite request) {
        return new AddVariant(request.sku(), request.name() == null ? request.sku() : request.name(),
            request.listPrice().amount(), request.listPrice().currency(), request.options(), request.weightGrams(),
            request.active() == null || request.active());
    }

    default Price price(PriceWrite request) {
        return new Price(request.listPrice().amount(), request.listPrice().currency(), request.reason());
    }

    default AddImage addImage(ImageWrite request) {
        return new AddImage(request.url(), request.altText(), request.sortOrder() == null ? 0 : request.sortOrder());
    }

    default CreateCategory createCategory(CategoryWrite request) {
        return new CreateCategory(request.parentId(), request.name(), request.imageUrl(),
            request.sortOrder() == null ? 0 : request.sortOrder(), request.featured() != null && request.featured());
    }

    default CategoryChange categoryChange(CategoryWrite request) {
        return new CategoryChange(request.name(), request.parentId(), request.imageUrl(),
            request.sortOrder() == null ? 0 : request.sortOrder(), request.featured() != null && request.featured());
    }

    default BulkAmendment bulkAmendment(BulkItem item) {
        return new BulkAmendment(item.productId(), productChange(item.amendment()));
    }

    // ---- command side: results -> views ----

    @Mapping(target = "ancestors", ignore = true)
    CategoryView categoryView(CategorySnapshot category);

    @Mapping(target = "availability", ignore = true)
    @Mapping(target = "listPrice.amount", source = "amount")
    @Mapping(target = "listPrice.currency", source = "currency")
    @Mapping(target = "promotionalPrice", ignore = true)
    VariantView variantView(VariantSnapshot variant);

    ProductImageView productImageView(ImageSnapshot image);

    @Mapping(target = "categories", expression = "java(java.util.List.of())")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", constant = "0")
    ProductDetailView productDetailView(ProductSnapshot product);

    BulkItemResult bulkItemResult(BulkAmendmentOutcome outcome);

    default BulkResult bulkResult(List<BulkAmendmentOutcome> outcomes) {
        return new BulkResult(outcomes.stream().map(this::bulkItemResult).toList());
    }
}
