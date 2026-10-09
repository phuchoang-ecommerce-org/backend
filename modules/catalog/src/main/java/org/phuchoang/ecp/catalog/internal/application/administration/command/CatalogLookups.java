package org.phuchoang.ecp.catalog.internal.application.administration.command;

import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;

import java.util.Optional;
import java.util.UUID;

/** The strict "must exist" loads shared by Catalog commands; idempotent commands do their own optional lookups. */
public final class CatalogLookups {

    private CatalogLookups() {
    }

    public static Product requireProduct(Optional<Product> product) {
        return product.orElseThrow(() -> notFound("Product not found."));
    }

    public static Product.Variant requireVariant(Product product, UUID variantId) {
        Product.Variant variant = product.variant(variantId);
        if (variant == null) {
            throw notFound("Variant not found.");
        }
        return variant;
    }

    public static Category requireCategory(Optional<Category> category) {
        return category.orElseThrow(() -> notFound("Category not found."));
    }

    /** {@code null} parent id means a root category. */
    public static Category optionalParent(UUID parentId, Optional<Category> parent) {
        return parentId == null ? null : requireCategory(parent);
    }

    public static ApplicationException invalid(String field) {
        return new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, "Invalid " + field + ".");
    }

    private static ApplicationException notFound(String detail) {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, detail);
    }
}
