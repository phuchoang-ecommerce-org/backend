package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.api.request.BulkItem;
import org.phuchoang.ecp.catalog.api.request.CategoryWrite;
import org.phuchoang.ecp.catalog.api.request.ImageWrite;
import org.phuchoang.ecp.catalog.api.request.PriceWrite;
import org.phuchoang.ecp.catalog.api.request.ProductWrite;
import org.phuchoang.ecp.catalog.api.request.PublicationWrite;
import org.phuchoang.ecp.catalog.api.request.VariantWrite;
import org.phuchoang.ecp.catalog.api.result.BulkResult;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.category.CreateCategoryService;
import org.phuchoang.ecp.catalog.internal.application.command.category.DeleteCategoryService;
import org.phuchoang.ecp.catalog.internal.application.command.category.UpdateCategoryService;
import org.phuchoang.ecp.catalog.internal.application.command.image.AddProductImageService;
import org.phuchoang.ecp.catalog.internal.application.command.image.RemoveProductImageService;
import org.phuchoang.ecp.catalog.internal.application.command.product.BulkAmendmentService;
import org.phuchoang.ecp.catalog.internal.application.command.product.CreateProductService;
import org.phuchoang.ecp.catalog.internal.application.command.product.DeleteProductService;
import org.phuchoang.ecp.catalog.internal.application.command.product.ProductPublicationService;
import org.phuchoang.ecp.catalog.internal.application.command.product.UpdateProductService;
import org.phuchoang.ecp.catalog.internal.application.command.variant.AddVariantService;
import org.phuchoang.ecp.catalog.internal.application.command.variant.ChangeVariantPriceService;
import org.phuchoang.ecp.catalog.internal.application.command.variant.RemoveVariantService;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Translates the public administration contract into Catalog commands and results — nothing else.
 * Identifier generation, defaults, slug derivation and bulk transaction semantics all live behind
 * the use-case services and {@link CatalogDtoMapper}.
 */
@Component
class CatalogAdministrationFacadeAdapter implements CatalogAdministrationFacade {

    private final CreateProductService createProduct;
    private final UpdateProductService updateProduct;
    private final DeleteProductService deleteProduct;
    private final ProductPublicationService publication;
    private final BulkAmendmentService bulkAmendment;
    private final AddVariantService addVariant;
    private final RemoveVariantService removeVariant;
    private final ChangeVariantPriceService changePrice;
    private final AddProductImageService addImage;
    private final RemoveProductImageService removeImage;
    private final CreateCategoryService createCategory;
    private final UpdateCategoryService updateCategory;
    private final DeleteCategoryService deleteCategory;
    private final CatalogDtoMapper mapper;

    CatalogAdministrationFacadeAdapter(CreateProductService createProduct, UpdateProductService updateProduct,
            DeleteProductService deleteProduct, ProductPublicationService publication,
            BulkAmendmentService bulkAmendment, AddVariantService addVariant, RemoveVariantService removeVariant,
            ChangeVariantPriceService changePrice, AddProductImageService addImage,
            RemoveProductImageService removeImage, CreateCategoryService createCategory,
            UpdateCategoryService updateCategory, DeleteCategoryService deleteCategory, CatalogDtoMapper mapper) {
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
        this.publication = publication;
        this.bulkAmendment = bulkAmendment;
        this.addVariant = addVariant;
        this.removeVariant = removeVariant;
        this.changePrice = changePrice;
        this.addImage = addImage;
        this.removeImage = removeImage;
        this.createCategory = createCategory;
        this.updateCategory = updateCategory;
        this.deleteCategory = deleteCategory;
        this.mapper = mapper;
    }

    @Override
    public ProductDetailView createProduct(IdentityActor caller, UUID correlationId, ProductWrite request) {
        return mapper.productDetailView(createProduct.create(context(caller, correlationId), mapper.createProduct(request)));
    }

    @Override
    public ProductDetailView updateProduct(IdentityActor caller, UUID correlationId, UUID productId, ProductWrite request) {
        return mapper.productDetailView(
            updateProduct.update(context(caller, correlationId), productId, mapper.productChange(request)));
    }

    @Override
    public void deleteProduct(IdentityActor caller, UUID correlationId, UUID productId) {
        deleteProduct.delete(context(caller, correlationId), productId);
    }

    @Override
    public ProductDetailView setPublication(IdentityActor caller, UUID correlationId, UUID productId,
            PublicationWrite request) {
        return mapper.productDetailView(
            publication.setPublication(context(caller, correlationId), productId, mapper.setPublication(request)));
    }

    @Override
    public VariantView addVariant(IdentityActor caller, UUID correlationId, UUID productId, VariantWrite request) {
        return mapper.variantView(addVariant.add(context(caller, correlationId), productId, mapper.addVariant(request)));
    }

    @Override
    public void removeVariant(IdentityActor caller, UUID correlationId, UUID productId, UUID variantId) {
        removeVariant.remove(context(caller, correlationId), productId, variantId);
    }

    @Override
    public VariantView changePrice(IdentityActor caller, UUID correlationId, UUID productId, UUID variantId,
            PriceWrite request) {
        return mapper.variantView(
            changePrice.changePrice(context(caller, correlationId), productId, variantId, mapper.price(request)));
    }

    @Override
    public ProductImageView addImage(IdentityActor caller, UUID correlationId, UUID productId, ImageWrite request) {
        return mapper.productImageView(addImage.add(context(caller, correlationId), productId, mapper.addImage(request)));
    }

    @Override
    public void removeImage(IdentityActor caller, UUID correlationId, UUID productId, UUID imageId) {
        removeImage.remove(context(caller, correlationId), productId, imageId);
    }

    @Override
    public CategoryView createCategory(IdentityActor caller, UUID correlationId, CategoryWrite request) {
        return mapper.categoryView(createCategory.create(context(caller, correlationId), mapper.createCategory(request)));
    }

    @Override
    public CategoryView updateCategory(IdentityActor caller, UUID correlationId, UUID categoryId, CategoryWrite request) {
        return mapper.categoryView(
            updateCategory.update(context(caller, correlationId), categoryId, mapper.categoryChange(request)));
    }

    @Override
    public void deleteCategory(IdentityActor caller, UUID correlationId, UUID categoryId) {
        deleteCategory.delete(context(caller, correlationId), categoryId);
    }

    @Override
    public BulkResult amendBulk(IdentityActor caller, UUID correlationId, List<BulkItem> items) {
        return mapper.bulkResult(
            bulkAmendment.amend(context(caller, correlationId), items.stream().map(mapper::bulkAmendment).toList()));
    }

    private static CatalogCommandContext context(IdentityActor caller, UUID correlationId) {
        return new CatalogCommandContext(caller, correlationId);
    }
}
