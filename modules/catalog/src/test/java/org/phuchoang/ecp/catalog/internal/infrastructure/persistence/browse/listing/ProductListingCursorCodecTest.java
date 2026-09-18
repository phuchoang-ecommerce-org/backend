package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingCursorCodec.SeekPosition;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorSigningKey;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.HmacCursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A cursor is bound to category, sort and filters, and carries one sort value plus the id tie-breaker. */
class ProductListingCursorCodecTest {

    private final ProductListingCursorCodec codec = new ProductListingCursorCodec(
        new HmacCursorCodec(CursorSigningKey.utf8("test-key", "01234567890123456789012345678901"), null));

    @Test
    void roundTripsTheLastRowOfAPage() {
        UUID categoryId = UUID.randomUUID();
        ProductListingQuery query = new ProductListingQuery(null, 2, "price:desc", List.of("Acme"), null, null, null);
        ProductListingRow last = row("Mug", new BigDecimal("42.00"));

        String encoded = codec.encode(categoryId, query, last);
        SeekPosition decoded = codec.decode(categoryId, withCursor(query, encoded));

        assertThat(decoded.productId()).isEqualTo(last.id());
        assertThat(decoded.sortValue()).isEqualTo(CursorValue.decimal(new BigDecimal("42.00")));
    }

    @Test
    void aCursorCannotBeReplayedWithDifferentFiltersSortOrCategory() {
        UUID categoryId = UUID.randomUUID();
        ProductListingQuery query = new ProductListingQuery(null, 2, "price:desc", List.of("Acme"), null, null, null);
        String encoded = codec.encode(categoryId, query, row("Mug", new BigDecimal("42.00")));

        assertThatThrownBy(() -> codec.decode(UUID.randomUUID(), withCursor(query, encoded)))
            .isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(categoryId, new ProductListingQuery(encoded, 2, "price:desc",
            List.of("Contoso"), null, null, null))).isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(categoryId, new ProductListingQuery(encoded, 2, "name:asc",
            List.of("Acme"), null, null, null))).isInstanceOf(InvalidCursorException.class);
    }

    @Test
    void aBlankCursorMeansTheFirstPage() {
        assertThat(codec.decode(UUID.randomUUID(),
            new ProductListingQuery(" ", 2, "default", List.of(), null, null, null))).isNull();
    }

    private static ProductListingQuery withCursor(ProductListingQuery query, String cursor) {
        return new ProductListingQuery(cursor, query.size(), query.sort(), query.brands(), query.priceFrom(),
            query.priceTo(), query.inStock());
    }

    private static ProductListingRow row(String name, BigDecimal priceFrom) {
        return new ProductListingRow(UUID.randomUUID(), name, "slug", "Acme", "PUBLISHED", null, priceFrom, null,
            "USD", null, 0, null, null);
    }
}
