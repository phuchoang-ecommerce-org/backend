package org.phuchoang.ecp.catalog.api.facade;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.api.query.CatalogListingQuery;
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
import org.phuchoang.ecp.catalog.internal.application.command.model.CategorySnapshot;
import org.phuchoang.ecp.catalog.internal.application.command.model.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.command.model.ProductSnapshot;
import org.phuchoang.ecp.catalog.internal.application.command.model.VariantSnapshot;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel;

/** Maps public catalog DTOs and application-owned read or command results. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CatalogDtoMapper {

    CatalogBrowseModel.ListingQuery listingQuery(CatalogListingQuery query);

    CategoryView categoryView(CatalogBrowseModel.Category category);

    CategoryNodeView categoryNodeView(CatalogBrowseModel.CategoryNode category);

    CategoryRefView categoryRefView(CatalogBrowseModel.CategoryRef category);

    MoneyView moneyView(CatalogBrowseModel.Money money);

    ProductSummaryView productSummaryView(CatalogBrowseModel.ProductSummary product);

    ProductPageView productPageView(CatalogBrowseModel.ProductPage page);

    @Mapping(target = "availability", source = "inStock")
    VariantView variantView(CatalogBrowseModel.Variant variant);

    ProductImageView productImageView(CatalogBrowseModel.ProductImage image);

    ProductDetailView productDetailView(CatalogBrowseModel.ProductDetail product);

    RatingSummaryView ratingSummaryView(CatalogBrowseModel.RatingSummary rating);

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

    default AdvisoryAvailabilityView advisoryAvailabilityView(Boolean inStock) {
        return inStock == null ? null : new AdvisoryAvailabilityView(inStock);
    }
}
