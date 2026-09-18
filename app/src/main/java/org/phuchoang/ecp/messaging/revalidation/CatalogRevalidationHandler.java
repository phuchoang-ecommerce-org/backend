package org.phuchoang.ecp.messaging.revalidation;

import org.phuchoang.ecp.catalog.api.facade.CatalogCacheInvalidator;
import org.phuchoang.ecp.messaging.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The integration policy for Catalog events: claim the event once, apply it only if it is newer
 * than what this aggregate already reached, invalidate the affected read caches, then forward it
 * to the storefront. All of it in one transaction — if forwarding fails, the claim and the cursor
 * roll back and Kafka redelivers the event (at-least-once, idempotent by construction).
 */
@Component
public class CatalogRevalidationHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogRevalidationHandler.class);
    private static final Set<String> INVALIDATING_EVENTS = Set.of("ProductPriceChanged", "ProductDiscontinued",
        "ProductPublished", "ProductUpdated", "VariantAdded", "CategoryChanged");

    private final ProcessedEventStore processedEvents;
    private final RevalidationOrderingStore ordering;
    private final CatalogCacheInvalidator cache;
    private final WebRevalidationGateway web;

    public CatalogRevalidationHandler(ProcessedEventStore processedEvents, RevalidationOrderingStore ordering,
            CatalogCacheInvalidator cache, WebRevalidationGateway web) {
        this.processedEvents = processedEvents;
        this.ordering = ordering;
        this.cache = cache;
        this.web = web;
    }

    @Transactional
    public void handle(EventEnvelope event, String originalBody) {
        if (!processedEvents.tryMarkProcessed(event.eventId(), event.eventType())) {
            return; // duplicate delivery
        }
        if (!ordering.advanceIfNewer(event.aggregateId(), event.occurredAt())) {
            log.info("Ignoring out-of-order catalog eventId={} aggregateId={}", event.eventId(), event.aggregateId());
            return;
        }
        invalidate(event.eventType(), event.payload());
        web.revalidate(event, originalBody);
    }

    private void invalidate(String type, JsonNode payload) {
        if (!INVALIDATING_EVENTS.contains(type)) {
            log.warn("Ignoring unsupported catalog revalidation eventType={}", type);
            return;
        }
        UUID productId = uuid(payload.path("productId"));
        if (productId != null) {
            cache.productChanged(productId);
        }
        List<UUID> variantIds = variantIds(payload);
        if (!variantIds.isEmpty()) {
            cache.variantsChanged(variantIds);
        }
        if (type.equals("CategoryChanged")) {
            cache.categoryTreeChanged();
        }
        List<UUID> categoryIds = uuids(payload.path("affectedCategoryIds"));
        if (!categoryIds.isEmpty()) {
            cache.categoryListingsChanged(categoryIds);
        }
    }

    /** {@code variantIds} is the v1 field; a single {@code variantId} and {@code product.variants[].id} are the fallbacks. */
    private static List<UUID> variantIds(JsonNode payload) {
        List<UUID> ids = new ArrayList<>(uuids(payload.path("variantIds")));
        if (ids.isEmpty()) {
            UUID single = uuid(payload.path("variantId"));
            if (single != null) {
                ids.add(single);
            }
            payload.path("product").path("variants").forEach(variant -> {
                UUID id = uuid(variant.path("id"));
                if (id != null) {
                    ids.add(id);
                }
            });
        }
        return List.copyOf(ids);
    }

    private static List<UUID> uuids(JsonNode array) {
        List<UUID> ids = new ArrayList<>();
        array.forEach(node -> {
            UUID id = uuid(node);
            if (id != null) {
                ids.add(id);
            }
        });
        return ids;
    }

    private static UUID uuid(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }
}
