package org.phuchoang.ecp.catalog.internal.application.command.model;

import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Application-owned command result, deliberately separate from the catalog aggregate. */
public record ProductSnapshot(UUID id, String name, String slug, String description, String brand,
                              String publicationStatus, OffsetDateTime publishedAt,
                              Map<String, Object> attributes, List<ImageSnapshot> images,
                              List<VariantSnapshot> variants) {

    public static ProductSnapshot from(Product product) {
        return new ProductSnapshot(product.id(), product.name(), product.slug(), product.description(), product.brand(),
            product.publicationStatus(), product.publishedAt() == null ? null : product.publishedAt().atOffset(ZoneOffset.UTC),
            product.attributes(), product.images().stream().map(ImageSnapshot::from).toList(),
            product.variants().stream().map(VariantSnapshot::from).toList());
    }
}
