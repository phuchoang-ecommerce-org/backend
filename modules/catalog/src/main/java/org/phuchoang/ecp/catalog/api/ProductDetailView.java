package org.phuchoang.ecp.catalog.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Complete guest-readable product detail. Review and recommendation sections are absent until owned. */
public record ProductDetailView(UUID id, String name, String slug, String description, String brand,
                                String publicationStatus, OffsetDateTime publishedAt,
                                List<CategoryRefView> categories, Map<String, Object> attributes,
                                List<ProductImageView> images, List<VariantView> variants,
                                Double averageRating, int reviewCount) {
}
