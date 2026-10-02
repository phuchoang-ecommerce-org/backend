package org.phuchoang.ecp.catalog.internal.application.browse;

import org.phuchoang.ecp.catalog.api.error.DomainException;
import org.phuchoang.ecp.catalog.api.error.GenErrorCode;

/** Catalog's uniform read-side not-found outcomes; an unpublished product is reported exactly like an absent one. */
public final class CatalogReadErrors {

    private CatalogReadErrors() {
    }

    public static DomainException categoryNotFound() {
        return new DomainException(GenErrorCode.NOT_FOUND, "Category not found.");
    }

    public static DomainException productNotFound() {
        return new DomainException(GenErrorCode.NOT_FOUND, "Product not found.");
    }

    public static DomainException variantNotFound() {
        return new DomainException(GenErrorCode.NOT_FOUND, "Variant not found.");
    }
}
