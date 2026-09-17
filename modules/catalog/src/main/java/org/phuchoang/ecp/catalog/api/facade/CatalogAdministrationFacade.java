package org.phuchoang.ecp.catalog.api.facade;

import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.catalog.api.request.*;
import org.phuchoang.ecp.catalog.api.result.BulkResult;
import org.phuchoang.ecp.identity.api.authorization.CallerContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Public command boundary for the catalog administration operations. */
public interface CatalogAdministrationFacade {
    /** Creates a draft product after authorizing the caller for catalog administration. */
    ProductDetailView createProduct(CallerContext caller, UUID correlationId, ProductWrite request);
    /** Replaces the mutable merchandising fields of an existing product. */
    ProductDetailView updateProduct(CallerContext caller, UUID correlationId, UUID productId, ProductWrite request);
    /** Discontinues a product so it is no longer available to new catalog reads or purchases. */
    void deleteProduct(CallerContext caller, UUID correlationId, UUID productId);
    /** Transitions a product's publication state and records the supplied reason. */
    ProductDetailView setPublication(CallerContext caller, UUID correlationId, UUID productId, PublicationWrite request);
    /** Adds one SKU-bearing variant while preserving catalog-wide SKU uniqueness. */
    VariantView addVariant(CallerContext caller, UUID correlationId, UUID productId, VariantWrite request);
    /** Removes a variant from a product's purchasable configuration set. */
    void removeVariant(CallerContext caller, UUID correlationId, UUID productId, UUID variantId);
    /** Replaces a variant's current list price without rewriting historical order prices. */
    VariantView changePrice(CallerContext caller, UUID correlationId, UUID productId, UUID variantId, PriceWrite request);
    /** Adds an ordered display image owned by the product aggregate. */
    ProductImageView addImage(CallerContext caller, UUID correlationId, UUID productId, ImageWrite request);
    /** Removes an aggregate-owned display image. */
    void removeImage(CallerContext caller, UUID correlationId, UUID productId, UUID imageId);
    /** Creates a category while enforcing the hierarchy invariants of {@code BR-CAT-03}. */
    CategoryView createCategory(CallerContext caller, UUID correlationId, CategoryWrite request);
    /** Updates a category and rejects an attempted cycle in its parent hierarchy. */
    CategoryView updateCategory(CallerContext caller, UUID correlationId, UUID categoryId, CategoryWrite request);
    /** Deletes an empty category only; callers must first resolve its products and child categories. */
    void deleteCategory(CallerContext caller, UUID correlationId, UUID categoryId);
    /** Applies independent product amendments and returns the outcome for every submitted item. */
    BulkResult amendBulk(CallerContext caller, UUID correlationId, List<BulkItem> items);

}
