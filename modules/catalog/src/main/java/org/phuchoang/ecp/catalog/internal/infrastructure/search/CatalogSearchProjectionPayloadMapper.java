package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import org.phuchoang.ecp.catalog.internal.application.search.projection.SearchProjectionEvent;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maps versioned Catalog event payloads to projection-only data, without naming Elasticsearch APIs. */
final class CatalogSearchProjectionPayloadMapper {

    private static final String PUBLISHED = "PUBLISHED";
    private static final String PRODUCT_DOCUMENT = "PRODUCT";

    private final Clock clock;
    private final ObjectMapper json;

    CatalogSearchProjectionPayloadMapper(Clock clock, ObjectMapper json) {
        this.clock = clock;
        this.json = json;
    }

    SearchProjectionOperation operation(SearchProjectionEvent event) {
        return switch (event.eventType()) {
            case "ProductCreated", "ProductUpdated", "ProductPublished" -> productSnapshot(event, payload(event).path("product"));
            case "ProductPriceChanged" -> new SearchProjectionOperation.VariantPriceChange(priceChange(event));
            case "VariantAdded" -> variantAdded(event);
            case "ProductDiscontinued" -> new SearchProjectionOperation.ProductTombstone();
            case "CategoryChanged" -> new SearchProjectionOperation.Ignore(event.eventType());
            default -> new SearchProjectionOperation.Ignore(event.eventType());
        };
    }

    private SearchProjectionOperation productSnapshot(SearchProjectionEvent event, JsonNode product) {
        if (product.isMissingNode() || product.isNull()) {
            throw new IllegalArgumentException("Catalog " + event.eventType() + " payload has no product snapshot.");
        }
        if (!PUBLISHED.equals(product.path("publicationStatus").asText())) {
            return new SearchProjectionOperation.ProductTombstone();
        }
        return new SearchProjectionOperation.ProductSnapshot(productDocument(event, product));
    }

    private SearchProjectionOperation variantAdded(SearchProjectionEvent event) {
        JsonNode snapshot = payload(event).path("product");
        return !snapshot.isMissingNode() && !snapshot.isNull()
            ? productSnapshot(event, snapshot)
            : new SearchProjectionOperation.VariantAddition(addedVariant(event));
    }

    private Map<String, Object> priceChange(SearchProjectionEvent event) {
        JsonNode payload = payload(event);
        Map<String, Object> variant = new HashMap<>();
        variant.put("variantId", requiredText(payload, "variantId", event));
        addMoney(variant, payload.path("listPrice"), event);
        return Map.copyOf(variant);
    }

    private Map<String, Object> addedVariant(SearchProjectionEvent event) {
        JsonNode payload = payload(event);
        Map<String, Object> variant = new HashMap<>();
        variant.put("variantId", requiredText(payload, "variantId", event));
        variant.put("sku", requiredText(payload.path("variantSkus").path(0), "", event));
        addMoney(variant, payload.path("listPrice"), event);
        return Map.copyOf(variant);
    }

    private Map<String, Object> productDocument(SearchProjectionEvent event, JsonNode product) {
        Map<String, Object> document = new HashMap<>();
        document.put("documentType", PRODUCT_DOCUMENT);
        document.put("productId", requiredText(product, "id", event));
        putIfPresent(document, "slug", product.path("slug"));
        putIfPresent(document, "name", product.path("name"));
        putIfPresent(document, "description", product.path("description"));
        putIfPresent(document, "brand", product.path("brand"));
        putIfPresent(document, "categoryId", product.path("categoryId"));
        putIfPresent(document, "publicationStatus", product.path("publicationStatus"));
        putIfPresent(document, "publishedAt", product.path("publishedAt"));
        primaryImageUrl(product.path("images")).ifPresent(url -> document.put("primaryImageUrl", url));
        document.put("attributes", map(product.path("attributes")));
        document.put("variants", variants(product.path("variants"), event));
        document.put("catalogEventAt", event.occurredAt().toString());
        document.put("indexedAt", clock.instant().toString());
        return Map.copyOf(document);
    }

    private static List<Map<String, Object>> variants(JsonNode variants, SearchProjectionEvent event) {
        List<Map<String, Object>> result = new ArrayList<>();
        variants.forEach(variant -> {
            Map<String, Object> projected = new HashMap<>();
            putIfPresent(projected, "variantId", variant.path("id"));
            putIfPresent(projected, "sku", variant.path("sku"));
            addMoney(projected, variant.path("listPrice"), event);
            projected.put("options", map(variant.path("options")));
            result.add(Map.copyOf(projected));
        });
        return List.copyOf(result);
    }

    private static java.util.Optional<String> primaryImageUrl(JsonNode images) {
        JsonNode primary = null;
        for (JsonNode image : images) {
            if (primary == null || image.path("sortOrder").asInt() < primary.path("sortOrder").asInt()) {
                primary = image;
            }
        }
        if (primary == null || primary.path("url").isMissingNode() || primary.path("url").isNull()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(primary.path("url").asText());
    }

    private static void addMoney(Map<String, Object> target, JsonNode money, SearchProjectionEvent event) {
        target.put("listPrice", new BigDecimal(requiredText(money, "amount", event)));
        target.put("currency", requiredText(money, "currency", event));
    }

    private static Map<String, Object> map(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        node.forEachEntry((name, value) -> result.put(name, scalar(value)));
        return Map.copyOf(result);
    }

    private static Object scalar(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isIntegralNumber()) return node.asLong();
        if (node.isFloatingPointNumber()) return node.decimalValue();
        return node.toString();
    }

    private static void putIfPresent(Map<String, Object> target, String field, JsonNode value) {
        if (!value.isMissingNode() && !value.isNull()) {
            target.put(field, value.asText());
        }
    }

    private static String requiredText(JsonNode source, String field, SearchProjectionEvent event) {
        JsonNode value = field.isEmpty() ? source : source.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            String prefix = event == null ? "Catalog payload" : "Catalog " + event.eventType() + " payload";
            throw new IllegalArgumentException(prefix + " has no " + (field.isEmpty() ? "required value" : field) + ".");
        }
        return value.asText();
    }

    private JsonNode payload(SearchProjectionEvent event) {
        try {
            return json.readTree(event.payload());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot parse Catalog " + event.eventType() + " projection payload.",
                exception);
        }
    }
}
