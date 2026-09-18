package org.phuchoang.ecp.catalog.internal.application.query.model.product;

import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryRef;
import org.phuchoang.ecp.catalog.internal.application.query.model.variant.VariantDetail;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The complete published product projection with ordered images and variants. */
public record ProductDetail(UUID id, String name, String slug, String description, String brand,
        String publicationStatus, OffsetDateTime publishedAt, List<CategoryRef> categories,
        Map<String, Object> attributes, List<ProductImage> images, List<VariantDetail> variants,
        Double averageRating, int reviewCount) {
}
