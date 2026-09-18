package org.phuchoang.ecp.catalog.internal.domain.repository;

/**
 * A save was rejected because a variant's SKU is already in use or retired (`BR-CAT-01`:
 * {@code ux_catalog_variant_sku} and the {@code catalog_retired_sku} trigger). Raised by the
 * persistence adapter so the application layer never sees a Spring persistence exception.
 */
public class DuplicateSkuException extends RuntimeException {

    private final String sku;

    public DuplicateSkuException(String sku, Throwable cause) {
        super("SKU '" + sku + "' is already in use or retired.", cause);
        this.sku = sku;
    }

    public String sku() {
        return sku;
    }
}
