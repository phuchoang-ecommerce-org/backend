package org.phuchoang.ecp.catalog.internal.application.administration.product.image.add;

/** Normalized input for adding one aggregate-owned product image; the service assigns the identifier. */
public record AddImage(String url, String altText, int sortOrder) {
}
