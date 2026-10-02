package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.search.SearchProductsQuery;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorSigningKey;
import org.phuchoang.ecp.catalog.internal.application.pagination.HmacCursorCodec;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchProductsCursorCodecTest {

    private final SearchProductsCursorCodec cursors = new SearchProductsCursorCodec(new HmacCursorCodec(
        CursorSigningKey.utf8("test", "a-secret-that-is-more-than-thirty-two-bytes"), null));

    @Test
    void roundTripsTheElasticsearchSortTupleAndBindsItToEveryFilter() {
        UUID productId = UUID.randomUUID();
        SearchProductsQuery query = new SearchProductsQuery("jacket", null, 20, "relevance", null,
            List.of("ECP"), Map.of("colour", "blue"), new BigDecimal("10.00"), new BigDecimal("20.00"), true);
        Hit<Object> hit = Hit.of(builder -> builder.index("ecp-products-v1").id(productId.toString())
            .sort(FieldValue.of(3.5d), FieldValue.of(productId.toString())));

        String cursor = cursors.encode(query, hit);

        assertThat(cursors.decode(new SearchProductsQuery("jacket", cursor, 20, "relevance", null,
            List.of("ECP"), Map.of("colour", "blue"), new BigDecimal("10"), new BigDecimal("20"), true)))
            .extracting(FieldValue::_toJsonString)
            .containsExactly("3.5", productId.toString());
        assertThatThrownBy(() -> cursors.decode(new SearchProductsQuery("jacket", cursor, 20, "relevance", null,
            List.of("other-brand"), Map.of("colour", "blue"), new BigDecimal("10"), new BigDecimal("20"), true)))
            .isInstanceOf(RuntimeException.class);
    }
}
