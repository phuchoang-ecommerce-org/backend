package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.api.view.category.CategoryRefView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.AdvisoryAvailabilityView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.catalog.api.request.*;
import org.phuchoang.ecp.catalog.api.result.*;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogAdministrationService;
import org.phuchoang.ecp.catalog.internal.application.command.model.*;
import org.phuchoang.ecp.identity.api.authorization.CallerContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Maps the external administration contract to catalog's internal command model. */
@Component
class CatalogAdministrationFacadeAdapter implements CatalogAdministrationFacade {
    private final CatalogAdministrationService service;
    private final CatalogDtoMapper mapper;
    CatalogAdministrationFacadeAdapter(CatalogAdministrationService service, CatalogDtoMapper mapper) { this.service = service; this.mapper = mapper; }
    @Override public ProductDetailView createProduct(CallerContext c, UUID x, ProductWrite r) { return mapper.productDetailView(service.createProduct(c, x, new CreateProduct(UUID.randomUUID(), r.name(), r.description(), r.brand(), r.categoryId(), r.attributes()))); }
    @Override public ProductDetailView updateProduct(CallerContext c, UUID x, UUID id, ProductWrite r) { return mapper.productDetailView(service.updateProduct(c, x, id, change(r, null))); }
    @Override public void deleteProduct(CallerContext c, UUID x, UUID id) { service.deleteProduct(c, x, id); }
    @Override public ProductDetailView setPublication(CallerContext c, UUID x, UUID id, PublicationWrite r) { return mapper.productDetailView(service.setPublication(c, x, id, r.publicationStatus(), r.reason())); }
    @Override public VariantView addVariant(CallerContext c, UUID x, UUID id, VariantWrite r) { return mapper.variantView(service.addVariant(c, x, id, new AddVariant(UUID.randomUUID(), r.sku(), r.name() == null ? r.sku() : r.name(), r.listPrice().amount(), r.listPrice().currency(), r.options(), r.weightGrams(), r.active() == null || r.active()))); }
    @Override public void removeVariant(CallerContext c, UUID x, UUID id, UUID variant) { service.removeVariant(c, x, id, variant); }
    @Override public VariantView changePrice(CallerContext c, UUID x, UUID id, UUID variant, PriceWrite r) { return mapper.variantView(service.changePrice(c, x, id, variant, new Price(r.listPrice().amount(), r.listPrice().currency()), r.reason())); }
    @Override public ProductImageView addImage(CallerContext c, UUID x, UUID id, ImageWrite r) { return mapper.productImageView(service.addImage(c, x, id, new AddImage(UUID.randomUUID(), r.url(), r.altText(), r.sortOrder() == null ? 0 : r.sortOrder()))); }
    @Override public void removeImage(CallerContext c, UUID x, UUID id, UUID image) { service.removeImage(c, x, id, image); }
    @Override public CategoryView createCategory(CallerContext c, UUID x, CategoryWrite r) { return mapper.categoryView(service.createCategory(c, x, new CreateCategory(UUID.randomUUID(), r.parentId(), r.name(), slug(r.name()), r.imageUrl(), r.sortOrder() == null ? 0 : r.sortOrder(), r.featured() != null && r.featured()))); }
    @Override public CategoryView updateCategory(CallerContext c, UUID x, UUID id, CategoryWrite r) { return mapper.categoryView(service.updateCategory(c, x, id, new CategoryChange(r.name(), r.parentId(), r.imageUrl(), r.sortOrder() == null ? 0 : r.sortOrder(), r.featured() != null && r.featured()))); }
    @Override public void deleteCategory(CallerContext c, UUID x, UUID id) { service.deleteCategory(c, x, id); }
    @Override public BulkResult amendBulk(CallerContext c, UUID x, List<BulkItem> items) { return new BulkResult(items.stream().map(i -> { try { updateProduct(c, x, i.productId(), i.amendment()); return new BulkItemResult(i.productId(), true, null, null); } catch (RuntimeException e) { return new BulkItemResult(i.productId(), false, "ECP-GEN-4000", e.getMessage()); } }).toList()); }
    private static ProductChange change(ProductWrite r, String status) { return new ProductChange(r.name(), r.description(), r.brand(), r.categoryId(), r.attributes(), status); }
    private static String slug(String value) { return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); }
}
