package org.phuchoang.ecp.web.catalog;

import org.phuchoang.ecp.catalog.api.CatalogProductFacade;
import org.phuchoang.ecp.catalog.api.ProductDetailView;
import org.phuchoang.ecp.catalog.api.RatingSummaryView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Guest-readable product detail and its independently available rating summary. */
@RestController
class ProductController {

    private final CatalogProductFacade catalog;

    ProductController(CatalogProductFacade catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/v1/products/{productId}")
    ProductDetailView getProduct(@PathVariable UUID productId) {
        return catalog.getProduct(productId);
    }

    @GetMapping("/api/v1/products/{productId}/rating-summary")
    RatingSummaryView getProductRatingSummary(@PathVariable UUID productId) {
        return catalog.getProductRatingSummary(productId);
    }
}
