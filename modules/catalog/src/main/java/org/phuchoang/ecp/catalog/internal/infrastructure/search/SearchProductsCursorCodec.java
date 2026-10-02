package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProductsQuery;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorCodec;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorContext;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorPosition;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorValue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HMAC-signed {@code search_after} cursor bound to every query and filter input. */
@Component
class SearchProductsCursorCodec {

    private static final String SCOPE = "catalog.search-products";

    private final CursorCodec codec;

    SearchProductsCursorCodec(CursorCodec codec) {
        this.codec = codec;
    }

    List<FieldValue> decode(SearchProductsQuery query) {
        if (query.cursor() == null || query.cursor().isBlank()) {
            return List.of();
        }
        CursorPosition position = codec.decode(query.cursor(), context(query));
        if (position.sortValues().size() != 1) {
            throw new IllegalArgumentException("Invalid search cursor.");
        }
        return List.of(fieldValue(position.sortValues().getFirst()), FieldValue.of(position.tieBreaker().toString()));
    }

    String encode(SearchProductsQuery query, Hit<?> lastHit) {
        List<FieldValue> values = lastHit.sort();
        if (values == null || values.size() != 2 || !values.get(1).isString()) {
            throw new IllegalStateException("Elasticsearch did not return the expected product search sort tuple.");
        }
        UUID productId = UUID.fromString(values.get(1).stringValue());
        return codec.encode(context(query), List.of(cursorValue(values.getFirst())), productId);
    }

    private static CursorContext context(SearchProductsQuery query) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("q", query.keyword() == null ? "" : query.keyword());
        filters.put("categoryId", query.categoryId() == null ? "" : query.categoryId().toString());
        filters.put("brands", String.join(",", query.brands()));
        query.attributes().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(attribute -> filters.put("attribute:" + attribute.getKey(), attribute.getValue()));
        filters.put("priceFrom", decimal(query.priceFrom()));
        filters.put("priceTo", decimal(query.priceTo()));
        filters.put("inStock", query.inStock() == null ? "" : query.inStock().toString());
        return new CursorContext(SCOPE, query.categoryId() == null ? "all" : query.categoryId().toString(), query.sort(), filters);
    }

    private static CursorValue cursorValue(FieldValue value) {
        if (value.isNull()) return CursorValue.nullValue();
        if (value.isString()) return CursorValue.text(value.stringValue());
        if (value.isLong()) return CursorValue.decimal(BigDecimal.valueOf(value.longValue()));
        if (value.isDouble()) return CursorValue.decimal(BigDecimal.valueOf(value.doubleValue()));
        throw new IllegalArgumentException("Unsupported Elasticsearch search sort value.");
    }

    private static FieldValue fieldValue(CursorValue value) {
        return switch (value.type()) {
            case NULL -> FieldValue.NULL;
            case TEXT -> FieldValue.of(value.textValue());
            case DECIMAL -> FieldValue.of(value.decimalValue().doubleValue());
            default -> throw new IllegalArgumentException("Invalid search cursor sort value.");
        };
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
