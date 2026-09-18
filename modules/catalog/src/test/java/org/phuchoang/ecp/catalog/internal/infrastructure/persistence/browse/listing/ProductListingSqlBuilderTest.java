package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingCursorCodec.SeekPosition;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingSqlBuilder.ListingSql;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** SQL composition in isolation: filters, count/page symmetry, seek predicates and ordering. */
class ProductListingSqlBuilderTest {

    @Test
    void firstPageHasNoSeekPredicateAndCountSharesTheFilters() {
        ProductListingQuery query = new ProductListingQuery(null, 20, "default", List.of("Acme"), null, null, true);

        ListingSql sql = ProductListingSqlBuilder.build("/c/", query, null);

        assertThat(sql.countSql()).startsWith(ProductListingSqlBuilder.PRODUCT_ROWS_CTE)
            .contains("SELECT COUNT(*) FROM product_rows WHERE 1 = 1 AND brand IN (?) AND in_stock = ?");
        assertThat(sql.countParameters()).containsExactly("/c/", "Acme", true);
        assertThat(sql.pageSql()).contains("ORDER BY name ASC, id ASC LIMIT ?").doesNotContain("id > ?");
        assertThat(sql.pageParameters()).containsExactly("/c/", "Acme", true, 21);
    }

    @Test
    void priceSeekTreatsNullPricesAsLastAndBindsTheTieBreaker() {
        ProductListingQuery query = new ProductListingQuery("c", 10, "price:asc", List.of(), null, null, null);
        UUID lastId = UUID.randomUUID();

        ListingSql withPrice = ProductListingSqlBuilder.build("/c/", query,
            new SeekPosition(CursorValue.decimal(new BigDecimal("9.99")), lastId));
        ListingSql nullPrice = ProductListingSqlBuilder.build("/c/", query,
            new SeekPosition(CursorValue.nullValue(), lastId));

        assertThat(withPrice.pageSql()).contains("(price_from IS NULL OR price_from > ? OR (price_from = ? AND id > ?))")
            .contains("ORDER BY price_from ASC NULLS LAST, id ASC");
        assertThat(withPrice.pageParameters()).containsExactly("/c/", new BigDecimal("9.99"), new BigDecimal("9.99"), lastId, 11);
        assertThat(nullPrice.pageSql()).contains("(price_from IS NULL AND id > ?)");
        assertThat(nullPrice.pageParameters()).containsExactly("/c/", lastId, 11);
    }

    @Test
    void otherSortsSeekOnTheirColumnWithTheIdTieBreaker() {
        Instant created = Instant.parse("2026-09-18T10:00:00Z");
        UUID lastId = UUID.randomUUID();
        ProductListingQuery query = new ProductListingQuery("c", 10, "createdAt:desc", List.of(), null, null, null);

        ListingSql sql = ProductListingSqlBuilder.build("/c/", query, new SeekPosition(CursorValue.instant(created), lastId));

        assertThat(sql.pageSql()).contains("(created_at < ? OR (created_at = ? AND id > ?))")
            .contains("ORDER BY created_at DESC, id ASC");
        assertThat(sql.pageParameters().get(1)).isInstanceOf(OffsetDateTime.class);
        assertThat(ProductListingSqlBuilder.orderBy("popularity:desc")).isEqualTo("review_count DESC, id ASC");
    }
}
