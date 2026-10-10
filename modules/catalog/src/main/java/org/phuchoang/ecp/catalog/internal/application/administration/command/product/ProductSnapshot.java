package org.phuchoang.ecp.catalog.internal.application.administration.command.product;

import org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.VariantSnapshot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application-owned command result, deliberately separate from the catalog aggregate. */
public record ProductSnapshot(UUID id, String name, String slug, String description, String brand,
                              String publicationStatus, OffsetDateTime publishedAt,
                              Map<String, Object> attributes, List<ImageSnapshot> images,
                              List<VariantSnapshot> variants) {
}
