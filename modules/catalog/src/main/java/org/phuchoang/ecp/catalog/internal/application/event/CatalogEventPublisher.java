package org.phuchoang.ecp.catalog.internal.application.event;

import tools.jackson.databind.ObjectMapper;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.event.*;
import org.phuchoang.ecp.sharedkernel.api.event.EventActor;
import org.phuchoang.ecp.sharedkernel.api.event.EventMetadata;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxEvent;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Converts Catalog business facts into the versioned Kafka outbox contract. */
@Component
public class CatalogEventPublisher {
    private static final String PRODUCT_TOPIC = "ecp.catalog.product.v1";
    private static final String CATEGORY_TOPIC = "ecp.catalog.category.v1";

    private final OutboxWriter outbox;
    private final Clock clock;
    private final ObjectMapper json;

    public CatalogEventPublisher(OutboxWriter outbox, Clock clock, ObjectMapper json) {
        this.outbox = outbox;
        this.clock = clock;
        this.json = json;
    }

    public void publish(CatalogDomainEvent event, UUID correlationId, EventActor actor,
            List<String> affectedCategorySlugs, Map<String, Object> categoryPayload) {
        Map<String, Object> payload = payload(event, affectedCategorySlugs, categoryPayload);
        outbox.append(new OutboxEvent(new EventMetadata(UUID.randomUUID(), event.eventType(), 1, clock.instant(),
            event.aggregateType(), event.aggregateId(), correlationId, actor), topic(event), json(payload)));
    }

    private static String topic(CatalogDomainEvent event) {
        return event instanceof CategoryChanged ? CATEGORY_TOPIC : PRODUCT_TOPIC;
    }

    private static Map<String, Object> payload(CatalogDomainEvent event, List<String> categorySlugs,
            Map<String, Object> categoryPayload) {
        if (event instanceof CategoryChanged) return new LinkedHashMap<>(categoryPayload);
        if (event instanceof ProductDiscontinued discontinued) {
            return Map.of("productId", discontinued.productId(), "variantSkus", discontinued.variantSkus());
        }
        if (event instanceof ProductPriceChanged changed) {
            return Map.of("productId", changed.productId(), "variantSkus", List.of(changed.variant().sku()),
                "variantId", changed.variant().id(), "listPrice", money(changed.variant()));
        }
        if (event instanceof VariantAdded added) {
            return productPayload(added.productId(), List.of(added.variant().sku()), categorySlugs,
                Map.of("variantId", added.variant().id(), "listPrice", money(added.variant())));
        }
        Product product = switch (event) {
            case ProductCreated created -> created.product();
            case ProductUpdated updated -> updated.product();
            case ProductPublished published -> published.product();
            default -> throw new IllegalArgumentException("Unsupported Catalog event " + event.getClass().getSimpleName());
        };
        return productPayload(product.id(), product.variants().stream().map(Product.Variant::sku).toList(), categorySlugs,
            Map.of("product", snapshot(product)));
    }

    private static Map<String, Object> productPayload(UUID productId, List<String> skus, List<String> categorySlugs,
            Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", productId);
        payload.put("variantSkus", skus);
        payload.put("affectedCategorySlugs", categorySlugs);
        payload.putAll(details);
        return payload;
    }

    private static Map<String, Object> money(Product.Variant variant) {
        return Map.of("amount", variant.amount().toPlainString(), "currency", variant.currency());
    }

    private static Map<String, Object> snapshot(Product product) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", product.id());
        value.put("categoryId", product.categoryId());
        value.put("name", product.name());
        value.put("slug", product.slug());
        value.put("description", product.description());
        value.put("brand", product.brand());
        value.put("publicationStatus", product.publicationStatus());
        value.put("publishedAt", product.publishedAt());
        value.put("attributes", product.attributes());
        value.put("variants", product.variants().stream().map(CatalogEventPublisher::snapshot).toList());
        value.put("images", product.images().stream().map(CatalogEventPublisher::snapshot).toList());
        return value;
    }

    private static Map<String, Object> snapshot(Product.Variant variant) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", variant.id()); value.put("sku", variant.sku()); value.put("name", variant.name());
        value.put("listPrice", money(variant)); value.put("options", variant.options());
        value.put("weightGrams", variant.weightGrams()); value.put("active", variant.active());
        return value;
    }

    private static Map<String, Object> snapshot(Product.Image image) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", image.id()); value.put("url", image.url()); value.put("altText", image.altText());
        value.put("sortOrder", image.sortOrder());
        return value;
    }

    private String json(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize catalog event payload", exception);
        }
    }
}
