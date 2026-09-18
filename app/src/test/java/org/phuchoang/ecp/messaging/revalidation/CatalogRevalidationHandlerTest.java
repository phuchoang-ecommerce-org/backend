package org.phuchoang.ecp.messaging.revalidation;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.api.facade.CatalogCacheInvalidator;
import org.phuchoang.ecp.messaging.EventEnvelope;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The integration policy — idempotency, ordering, invalidation, forwarding — without Kafka, Redis or HTTP. */
class CatalogRevalidationHandlerTest {

    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final RevalidationOrderingStore ordering = mock(RevalidationOrderingStore.class);
    private final CatalogCacheInvalidator cache = mock(CatalogCacheInvalidator.class);
    private final WebRevalidationGateway web = mock(WebRevalidationGateway.class);
    private final CatalogRevalidationHandler handler = new CatalogRevalidationHandler(processed, ordering, cache, web);
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void aDuplicateDeliveryIsIgnoredEntirely() {
        EventEnvelope event = event("ProductUpdated", "{\"productId\":\"" + UUID.randomUUID() + "\"}");
        when(processed.tryMarkProcessed(event.eventId(), "ProductUpdated")).thenReturn(false);

        handler.handle(event, "body");

        verifyNoInteractions(ordering, cache, web);
    }

    @Test
    void anOlderEventThanTheAggregateCursorIsIgnored() {
        EventEnvelope event = event("ProductUpdated", "{\"productId\":\"" + UUID.randomUUID() + "\"}");
        when(processed.tryMarkProcessed(any(), any())).thenReturn(true);
        when(ordering.advanceIfNewer(event.aggregateId(), event.occurredAt())).thenReturn(false);

        handler.handle(event, "body");

        verifyNoInteractions(cache, web);
    }

    @Test
    void aNewEventInvalidatesEveryAffectedEntryThenForwardsTheOriginalBody() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        EventEnvelope event = event("VariantAdded", "{\"productId\":\"" + productId + "\",\"variantIds\":[\"" + variantId
            + "\"],\"variantSkus\":[\"SKU\"],\"affectedCategoryIds\":[\"" + categoryId + "\"],\"affectedCategorySlugs\":[\"s\"]}");
        when(processed.tryMarkProcessed(any(), any())).thenReturn(true);
        when(ordering.advanceIfNewer(any(), any())).thenReturn(true);

        handler.handle(event, "raw-body");

        var order = inOrder(cache, web);
        order.verify(cache).productChanged(productId);
        order.verify(cache).variantsChanged(List.of(variantId));
        order.verify(cache).categoryListingsChanged(List.of(categoryId));
        order.verify(web).revalidate(event, "raw-body");
        verify(cache, never()).categoryTreeChanged();
    }

    @Test
    void aCategoryChangeAlsoDropsTheTreeAndOlderPayloadsFallBackToNestedVariantIds() {
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID nestedVariantId = UUID.randomUUID();
        EventEnvelope category = event("CategoryChanged", "{\"id\":\"" + categoryId + "\",\"affectedCategoryIds\":[\""
            + categoryId + "\"]}");
        EventEnvelope legacy = event("ProductPublished", "{\"productId\":\"" + productId + "\",\"product\":{\"variants\":[{\"id\":\""
            + nestedVariantId + "\"}]}}");
        when(processed.tryMarkProcessed(any(), any())).thenReturn(true);
        when(ordering.advanceIfNewer(any(), any())).thenReturn(true);

        handler.handle(category, "c");
        handler.handle(legacy, "l");

        verify(cache).categoryTreeChanged();
        verify(cache).categoryListingsChanged(List.of(categoryId));
        verify(cache).productChanged(productId);
        verify(cache).variantsChanged(List.of(nestedVariantId));
    }

    @Test
    void anUnsupportedTypeStillForwardsWithoutInvalidating() {
        EventEnvelope event = event("ProductCreated", "{\"productId\":\"" + UUID.randomUUID() + "\"}");
        when(processed.tryMarkProcessed(any(), any())).thenReturn(true);
        when(ordering.advanceIfNewer(any(), any())).thenReturn(true);

        handler.handle(event, "body");

        verifyNoInteractions(cache);
        verify(web).revalidate(event, "body");
    }

    @Test
    void aCallbackFailurePropagatesSoTheClaimRollsBackAndKafkaRedelivers() {
        EventEnvelope event = event("ProductUpdated", "{\"productId\":\"" + UUID.randomUUID() + "\"}");
        when(processed.tryMarkProcessed(any(), any())).thenReturn(true);
        when(ordering.advanceIfNewer(any(), any())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("HTTP 500")).when(web).revalidate(any(), any());

        assertThatThrownBy(() -> handler.handle(event, "body")).isInstanceOf(IllegalStateException.class);
    }

    private EventEnvelope event(String type, String payload) {
        return new EventEnvelope(UUID.randomUUID(), type, 1, Instant.parse("2026-09-18T10:00:00Z"), "Product",
            UUID.randomUUID(), UUID.randomUUID(), null, json.readTree(payload));
    }
}
