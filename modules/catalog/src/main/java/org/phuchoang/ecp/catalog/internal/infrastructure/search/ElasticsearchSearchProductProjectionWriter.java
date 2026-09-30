package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonData;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProjectionLagProbe;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProductProjectionWriter;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProjectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * The only Catalog writer for {@code ecp-products-*}.  Every operation is one guarded Elasticsearch
 * {@code _update}: the document's {@code catalogEventAt} is both the idempotency/order guard and
 * the source for projection-lag observability.
 */
@Component
class ElasticsearchSearchProductProjectionWriter implements SearchProductProjectionWriter, SearchProjectionLagProbe {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSearchProductProjectionWriter.class);
    private static final String ADVANCE_GUARD = "if (ctx._source.catalogEventAt == null || "
        + "ctx._source.catalogEventAt.compareTo(params.occurredAt) < 0) { ";
    private static final String NOOP = " } else { ctx.op = 'noop'; }";
    private static final String PRODUCT_DELTA_GUARD = ADVANCE_GUARD
        + "if (ctx._source.documentType == 'PRODUCT') { ";
    private static final String PRODUCT_DELTA_END = " } else { ctx._source.documentType = 'TOMBSTONE'; }"
        + " ctx._source.catalogEventAt = params.occurredAt; ctx._source.indexedAt = params.indexedAt;" + NOOP;

    private final ElasticsearchClient client;
    private final Clock clock;
    private final CatalogSearchProjectionPayloadMapper payloads;

    ElasticsearchSearchProductProjectionWriter(ElasticsearchClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
        this.payloads = new CatalogSearchProjectionPayloadMapper(clock);
    }

    @Override
    public void write(SearchProjectionEvent event) {
        SearchProjectionOperation operation = payloads.operation(event);
        switch (operation) {
            case SearchProjectionOperation.ProductSnapshot snapshot -> projectProductSnapshot(event, snapshot.document());
            case SearchProjectionOperation.VariantPriceChange priceChange -> projectPriceChange(event, priceChange.variant());
            case SearchProjectionOperation.VariantAddition variantAddition -> projectVariantAdded(event, variantAddition.variant());
            case SearchProjectionOperation.ProductTombstone ignored -> projectTombstone(event);
            case SearchProjectionOperation.Ignore ignored -> log.debug(
                "Catalog search eventId={} eventType={} has no product document delta", event.eventId(), event.eventType());
        }
    }

    @Override
    public double lagSeconds() {
        try {
            Double epochMillis = client.search(search -> search.index(SearchIndex.ALIAS).size(0)
                .aggregations("latest_catalog_event", aggregation -> aggregation.max(max -> max.field("catalogEventAt"))), Void.class)
                .aggregations().get("latest_catalog_event").max().value();
            if (epochMillis == null || epochMillis.isInfinite() || epochMillis.isNaN()) {
                return Double.NaN;
            }
            return Math.max(0, clock.instant().toEpochMilli() - epochMillis) / 1_000d;
        } catch (Exception unavailable) {
            log.debug("Cannot read product-search projection lag", unavailable);
            return Double.NaN;
        }
    }

    private void projectProductSnapshot(SearchProjectionEvent event, Map<String, Object> document) {
        guardedDocumentUpdate(event, document, ADVANCE_GUARD + "ctx._source.putAll(params.document);" + NOOP);
    }

    private void projectPriceChange(SearchProjectionEvent event, Map<String, Object> variant) {
        Map<String, JsonData> params = eventParams(event);
        params.put("variant", JsonData.of(variant));
        guardedUpdate(event, params, PRODUCT_DELTA_GUARD
            + " for (v in ctx._source.variants) { if (v.variantId == params.variant.variantId) { "
            + "v.listPrice = params.variant.listPrice; v.currency = params.variant.currency; } }"
            + PRODUCT_DELTA_END);
    }

    private void projectVariantAdded(SearchProjectionEvent event, Map<String, Object> variant) {
        Map<String, JsonData> params = eventParams(event);
        params.put("variant", JsonData.of(variant));
        guardedUpdate(event, params, PRODUCT_DELTA_GUARD
            + " boolean found = false; for (v in ctx._source.variants) { if (v.variantId == params.variant.variantId) { found = true; } }"
            + " if (!found) { ctx._source.variants.add(params.variant); }"
            + PRODUCT_DELTA_END);
    }

    /**
     * Retains only the ordering mark after withdrawal. Physical deletion would let an older event
     * scripted-upsert the product again because the high-water mark would be gone.
     */
    private void projectTombstone(SearchProjectionEvent event) {
        guardedUpdate(event, eventParams(event), ADVANCE_GUARD
            + "ctx._source.clear(); ctx._source.documentType = 'TOMBSTONE';"
            + "ctx._source.catalogEventAt = params.occurredAt; ctx._source.indexedAt = params.indexedAt;" + NOOP);
    }

    private void guardedDocumentUpdate(SearchProjectionEvent event, Map<String, Object> document, String source) {
        Map<String, JsonData> params = eventParams(event);
        params.put("document", JsonData.of(document));
        guardedUpdate(event, params, source);
    }

    private void guardedUpdate(SearchProjectionEvent event, Map<String, JsonData> params, String source) {
        try {
            client.update(request -> request.index(SearchIndex.ALIAS).id(event.aggregateId().toString()).retryOnConflict(5)
                .scriptedUpsert(true).upsert(Map.<String, Object>of())
                .script(script -> script.source(scriptSource -> scriptSource.scriptString(source)).params(params)), Map.class);
        } catch (Exception failed) {
            throw new IllegalStateException("Cannot project catalog event " + event.eventId() + " into product search", failed);
        }
    }

    private Map<String, JsonData> eventParams(SearchProjectionEvent event) {
        return new HashMap<>(Map.of(
            "occurredAt", JsonData.of(event.occurredAt().toString()),
            "indexedAt", JsonData.of(clock.instant().toString())));
    }
}
