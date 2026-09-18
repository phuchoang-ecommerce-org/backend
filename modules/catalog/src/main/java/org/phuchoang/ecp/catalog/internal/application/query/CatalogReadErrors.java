package org.phuchoang.ecp.catalog.internal.application.query;

import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;

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
