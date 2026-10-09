package org.phuchoang.ecp.catalog.internal.domain.error;

/**
 * A save was rejected because a variant's SKU is already in use or retired (`BR-CAT-01`). Raised
 * by the persistence adapter so application code never depends on Spring persistence exceptions.
 */
public final class DuplicateSkuException extends RuntimeException {

    private final String sku;

    public DuplicateSkuException(String sku, Throwable cause) {
        super("SKU '" + sku + "' is already in use or retired.", cause);
        this.sku = sku;
    }

    public String sku() {
        return sku;
    }
}
