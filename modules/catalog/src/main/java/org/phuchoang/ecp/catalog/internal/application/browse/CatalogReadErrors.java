package org.phuchoang.ecp.catalog.internal.application.browse;

import org.phuchoang.ecp.catalog.internal.application.error.ApplicationErrorCode;
import org.phuchoang.ecp.catalog.internal.application.error.ApplicationException;

/** Catalog's uniform read-side not-found outcomes; an unpublished product is reported exactly like an absent one. */
public final class CatalogReadErrors {

    private CatalogReadErrors() {
    }

    public static ApplicationException categoryNotFound() {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Category not found.");
    }

    public static ApplicationException productNotFound() {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Product not found.");
    }

    public static ApplicationException variantNotFound() {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, "Variant not found.");
    }
}
