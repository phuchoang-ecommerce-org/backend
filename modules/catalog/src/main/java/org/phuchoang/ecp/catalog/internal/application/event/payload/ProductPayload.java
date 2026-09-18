package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The product as the v1 contract publishes it — a deliberate copy of the aggregate's shape, so the
 * aggregate can evolve without silently changing the Kafka payload.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ProductPayload(UUID id, UUID categoryId, String name, String slug, String description, String brand,
        String publicationStatus, Instant publishedAt, Map<String, Object> attributes, List<VariantPayload> variants,
        List<ImagePayload> images) {

    public static ProductPayload of(Product product) {
        return new ProductPayload(product.id(), product.categoryId(), product.name(), product.slug(),
            product.description(), product.brand(), product.publicationStatus().name(), product.publishedAt(),
            product.attributes(), product.variants().stream().map(VariantPayload::of).toList(),
            product.images().stream().map(ImagePayload::of).toList());
    }
}
