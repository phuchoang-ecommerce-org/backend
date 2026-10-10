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
import org.phuchoang.ecp.catalog.internal.application.administration.command.category.update.CategoryChange;
import org.phuchoang.ecp.catalog.internal.application.administration.command.category.CategorySnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.category.create.CreateCategory;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.add.AddImage;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.bulk.BulkAmendment;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.bulk.BulkAmendmentOutcome;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.create.CreateProduct;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.update.ProductChange;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.ProductSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.publication.SetPublication;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.add.AddVariant;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.changeprice.Price;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.VariantSnapshot;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryDetail;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryNode;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryRef;
import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.browse.query.product.ProductDetail;
import org.phuchoang.ecp.catalog.internal.application.browse.query.product.ProductImage;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductSummary;
import org.phuchoang.ecp.catalog.internal.application.browse.query.product.RatingSummary;
import org.phuchoang.ecp.catalog.internal.application.browse.query.variant.VariantDetail;

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

    @Mapping(target = "name", expression = "java(request.name() == null ? request.sku() : request.name())")
    @Mapping(target = "amount", source = "listPrice.amount")
    @Mapping(target = "currency", source = "listPrice.currency")
    @Mapping(target = "active", expression = "java(request.active() == null || request.active())")
    AddVariant addVariant(VariantWrite request);

    @Mapping(target = "amount", source = "listPrice.amount")
    @Mapping(target = "currency", source = "listPrice.currency")
    Price price(PriceWrite request);

    @Mapping(target = "sortOrder", expression = "java(request.sortOrder() == null ? 0 : request.sortOrder())")
    AddImage addImage(ImageWrite request);

    @Mapping(target = "sortOrder", expression = "java(request.sortOrder() == null ? 0 : request.sortOrder())")
    @Mapping(target = "featured", expression = "java(request.featured() != null && request.featured())")
    CreateCategory createCategory(CategoryWrite request);

    @Mapping(target = "sortOrder", expression = "java(request.sortOrder() == null ? 0 : request.sortOrder())")
    @Mapping(target = "featured", expression = "java(request.featured() != null && request.featured())")
    CategoryChange categoryChange(CategoryWrite request);

    @Mapping(target = "change", source = "amendment")
    BulkAmendment bulkAmendment(BulkItem item);

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
