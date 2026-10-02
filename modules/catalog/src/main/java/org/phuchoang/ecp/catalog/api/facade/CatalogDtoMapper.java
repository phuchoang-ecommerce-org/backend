package org.phuchoang.ecp.catalog.api.facade;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.api.browse.categoryproduct.CatalogListingQuery;
import org.phuchoang.ecp.catalog.api.administration.product.BulkItem;
import org.phuchoang.ecp.catalog.api.administration.category.CategoryWrite;
import org.phuchoang.ecp.catalog.api.administration.product.ImageWrite;
import org.phuchoang.ecp.catalog.api.administration.product.PriceWrite;
import org.phuchoang.ecp.catalog.api.administration.product.ProductWrite;
import org.phuchoang.ecp.catalog.api.administration.product.PublicationWrite;
import org.phuchoang.ecp.catalog.api.administration.product.VariantWrite;
import org.phuchoang.ecp.catalog.api.administration.product.bulk.BulkItemResult;
import org.phuchoang.ecp.catalog.api.administration.product.bulk.BulkResult;
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
import org.phuchoang.ecp.catalog.internal.application.administration.category.update.CategoryChange;
import org.phuchoang.ecp.catalog.internal.application.administration.category.CategorySnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.category.create.CreateCategory;
import org.phuchoang.ecp.catalog.internal.application.administration.product.image.add.AddImage;
import org.phuchoang.ecp.catalog.internal.application.administration.product.image.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.product.bulk.BulkAmendment;
import org.phuchoang.ecp.catalog.internal.application.administration.product.bulk.BulkAmendmentOutcome;
import org.phuchoang.ecp.catalog.internal.application.administration.product.create.CreateProduct;
import org.phuchoang.ecp.catalog.internal.application.administration.product.update.ProductChange;
import org.phuchoang.ecp.catalog.internal.application.administration.product.ProductSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.product.publication.SetPublication;
import org.phuchoang.ecp.catalog.internal.application.administration.product.variant.add.AddVariant;
import org.phuchoang.ecp.catalog.internal.application.administration.product.variant.changeprice.Price;
import org.phuchoang.ecp.catalog.internal.application.administration.product.variant.VariantSnapshot;
import org.phuchoang.ecp.catalog.internal.application.browse.category.CategoryDetail;
import org.phuchoang.ecp.catalog.internal.application.browse.category.CategoryNode;
import org.phuchoang.ecp.catalog.internal.application.browse.category.CategoryRef;
import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.browse.product.ProductDetail;
import org.phuchoang.ecp.catalog.internal.application.browse.product.ProductImage;
import org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.browse.categoryproduct.ProductSummary;
import org.phuchoang.ecp.catalog.internal.application.browse.product.RatingSummary;
import org.phuchoang.ecp.catalog.internal.application.browse.variant.VariantDetail;

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
