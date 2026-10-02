package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProjectionEvent;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSearchProjectionPayloadMapperTest {

    private final ObjectMapper json = new ObjectMapper();
    private final CatalogSearchProjectionPayloadMapper mapper = new CatalogSearchProjectionPayloadMapper(
        Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC), json);

    @Test
    void mapsPublishedSnapshotsToACompleteSearchDocument() {
        UUID productId = UUID.randomUUID();
        SearchProjectionOperation operation = mapper.operation(event("ProductPublished", productId, """
            {"product":{"id":"%s","name":"Trail jacket","slug":"trail-jacket","brand":"ECP",
            "publicationStatus":"PUBLISHED","publishedAt":"2026-09-30T10:00:00Z","attributes":{"colour":"blue"},
            "variants":[{"id":"%s","sku":"JACKET-M","listPrice":{"amount":"10.50","currency":"USD"},
            "options":{"size":"M"}}],"images":[{"url":"https://example.test/first","sortOrder":1}]}}"""
            .formatted(productId, UUID.randomUUID())));

        assertThat(operation).isInstanceOf(SearchProjectionOperation.ProductSnapshot.class);
        Map<String, Object> document = ((SearchProjectionOperation.ProductSnapshot) operation).document();
        assertThat(document).containsEntry("documentType", "PRODUCT").containsEntry("productId", productId.toString())
            .containsEntry("primaryImageUrl", "https://example.test/first");
        @SuppressWarnings("unchecked")
        Map<String, Object> variant = ((java.util.List<Map<String, Object>>) document.get("variants")).getFirst();
        assertThat(variant).containsEntry("sku", "JACKET-M").containsEntry("listPrice", new BigDecimal("10.50"));
    }

    @Test
    void mapsUnpublishedAndDiscontinuedProductsToTombstonesThatRetainTheOrderingMark() {
        UUID productId = UUID.randomUUID();

        SearchProjectionOperation unpublished = mapper.operation(event("ProductUpdated", productId,
            "{\"product\":{\"id\":\"%s\",\"publicationStatus\":\"UNPUBLISHED\"}}".formatted(productId)));
        SearchProjectionOperation discontinued = mapper.operation(event("ProductDiscontinued", productId, "{}"));

        assertThat(unpublished).isInstanceOf(SearchProjectionOperation.ProductTombstone.class);
        assertThat(discontinued).isInstanceOf(SearchProjectionOperation.ProductTombstone.class);
    }

    @Test
    void mapsTheBackwardCompatibleVariantDeltaWithoutReadingCatalogState() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        SearchProjectionOperation operation = mapper.operation(event("VariantAdded", productId, """
            {"variantId":"%s","variantSkus":["JACKET-M"],"listPrice":{"amount":"10.50","currency":"USD"}}"""
            .formatted(variantId)));

        assertThat(operation).isInstanceOf(SearchProjectionOperation.VariantAddition.class);
        assertThat(((SearchProjectionOperation.VariantAddition) operation).variant())
            .containsEntry("variantId", variantId.toString()).containsEntry("sku", "JACKET-M")
            .containsEntry("listPrice", new BigDecimal("10.50"));
    }

    private SearchProjectionEvent event(String eventType, UUID aggregateId, String payload) {
        return new SearchProjectionEvent(UUID.randomUUID(), eventType, Instant.parse("2026-09-30T11:00:00Z"), "Product",
            aggregateId, UUID.randomUUID(), payload);
    }
}
