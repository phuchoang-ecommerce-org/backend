package org.phuchoang.ecp.catalog.internal.application.command;

import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;

import java.util.UUID;

/** The strict "must exist" loads shared by Catalog commands; idempotent commands do their own optional lookups. */
public final class CatalogLookups {

    private CatalogLookups() {
    }

    public static Product requireProduct(ProductRepository products, UUID productId) {
        return products.findById(productId).orElseThrow(() -> notFound("Product not found."));
    }

    public static Product.Variant requireVariant(Product product, UUID variantId) {
        Product.Variant variant = product.variant(variantId);
        if (variant == null) {
            throw notFound("Variant not found.");
        }
        return variant;
    }

    public static Category requireCategory(CategoryRepository categories, UUID categoryId) {
        return categories.findById(categoryId).orElseThrow(() -> notFound("Category not found."));
    }

    /** {@code null} parent id means a root category. */
    public static Category optionalParent(CategoryRepository categories, UUID parentId) {
        return parentId == null ? null : requireCategory(categories, parentId);
    }

    public static DomainException invalid(String field) {
        return new DomainException(GenErrorCode.VALIDATION_FAILED, "Invalid " + field + ".");
    }

    private static DomainException notFound(String detail) {
        return new DomainException(GenErrorCode.NOT_FOUND, detail);
    }
}
