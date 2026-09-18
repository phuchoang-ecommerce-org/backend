package org.phuchoang.ecp.catalog.internal.application.port;

import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductDetail;

import java.util.Optional;
import java.util.UUID;

/** Read port for a single published product. */
public interface ProductDetailPort {

    /** Whether the product exists <em>and</em> is published — what guest reads may see (`BR-CAT-02`). */
    boolean publishedProductExists(UUID id);

    /** The published product projection with ordered children; empty for absent and unpublished alike. */
    Optional<ProductDetail> product(UUID id);
}
