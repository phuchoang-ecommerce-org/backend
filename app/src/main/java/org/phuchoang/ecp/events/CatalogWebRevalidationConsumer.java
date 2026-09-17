package org.phuchoang.ecp.events;

import org.phuchoang.ecp.sharedkernel.api.cache.CacheAside;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/** The single event-driven path from catalog writes to API cache and web revalidation. */
@Component
class CatalogWebRevalidationConsumer {
    private static final Logger log = LoggerFactory.getLogger(CatalogWebRevalidationConsumer.class);
    private final JdbcClient jdbc;
    private final CacheAside cache;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newHttpClient();
    private final String callbackUrl;
    private final String secret;

    CatalogWebRevalidationConsumer(JdbcClient jdbc, CacheAside cache, ObjectMapper json,
            @Value("${ecp.revalidation.url:}") String callbackUrl,
            @Value("${ecp.revalidation.secret:}") String secret) {
        this.jdbc = jdbc; this.cache = cache; this.json = json; this.callbackUrl = callbackUrl; this.secret = secret;
    }

    @KafkaListener(topics = { "ecp.catalog.product.v1", "ecp.catalog.category.v1" },
        groupId = "${ecp.revalidation.consumer-group:ecp.web-revalidation}")
    @Transactional
    void consume(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = json.readTree(record.value());
        UUID eventId = UUID.fromString(envelope.path("eventId").asText());
        UUID aggregateId = UUID.fromString(envelope.path("aggregateId").asText());
        String type = envelope.path("eventType").asText();
        String occurredAt = envelope.path("occurredAt").asText();
        int claimed = jdbc.sql("""
            INSERT INTO catalog_processed_event (event_id, event_type) VALUES (:eventId, :eventType)
            ON CONFLICT DO NOTHING
            """).param("eventId", eventId).param("eventType", type).update();
        if (claimed == 0) return;
        int current = jdbc.sql("""
            INSERT INTO catalog_revalidation_cursor (aggregate_id, occurred_at) VALUES (:aggregateId, CAST(:occurredAt AS timestamptz))
            ON CONFLICT (aggregate_id) DO UPDATE SET occurred_at = EXCLUDED.occurred_at
            WHERE catalog_revalidation_cursor.occurred_at < EXCLUDED.occurred_at
            """).param("aggregateId", aggregateId).param("occurredAt", occurredAt).update();
        if (current == 0) { log.info("Ignoring out-of-order catalog eventId={} aggregateId={}", eventId, aggregateId); return; }
        invalidate(type, envelope.path("payload"));
        forward(record.value(), envelope.path("correlationId").asText(), eventId);
    }

    private void invalidate(String type, JsonNode payload) {
        if (!switch (type) {
            case "ProductPriceChanged", "ProductDiscontinued", "ProductPublished", "ProductUpdated", "VariantAdded", "CategoryChanged" -> true;
            default -> false;
        }) {
            log.warn("Ignoring unsupported catalog revalidation eventType={}", type);
            return;
        }
        String productId = payload.path("productId").asText(null);
        if (productId != null) cache.invalidate("cat:product:" + productId);
        payload.path("variantSkus").forEach(sku -> cache.invalidate("variant:" + sku.asText()));
        if (type.equals("CategoryChanged")) cache.invalidate("category-tree");
        if (type.equals("CategoryChanged") || type.equals("ProductPublished") || type.equals("ProductUpdated")
                || type.equals("VariantAdded")) {
            payload.path("affectedCategorySlugs").forEach(slug -> cache.invalidate("category-listing:" + slug.asText()));
        }
    }

    private void forward(String body, String correlationId, UUID eventId) throws Exception {
        if (callbackUrl.isBlank() || secret.isBlank()) {
            throw new IllegalStateException("Catalog revalidation callback is not configured.");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(callbackUrl)).header("Content-Type", "application/json")
            .header("X-ECP-Signature", signature(body)).header("X-Correlation-Id", correlationId)
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status == 204) return;
        if (status == 401) throw new IllegalStateException("Web revalidation signature rejected for eventId=" + eventId);
        throw new IllegalStateException("Web revalidation returned HTTP " + status + " for eventId=" + eventId);
    }

    private String signature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
