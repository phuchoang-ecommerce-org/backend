package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorPosition;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Keyset cursor semantics for category listings: a cursor is bound to the category, the sort key
 * and the complete filter set, so one produced for one query cannot be replayed against another;
 * it carries one sort value plus the product id as tie-breaker.
 */
final class ProductListingCursorCodec {

    private static final String SCOPE = "catalog.category-products";

    private final CursorCodec codec;

    ProductListingCursorCodec(CursorCodec codec) {
        this.codec = codec;
    }

    /** @return the decoded seek position, or {@code null} for a first page */
    SeekPosition decode(UUID categoryId, ProductListingQuery query) {
        if (query.cursor() == null || query.cursor().isBlank()) {
            return null;
        }
        CursorPosition position = codec.decode(query.cursor(), context(categoryId, query));
        if (position.sortValues().size() != 1) {
            throw new InvalidCursorException();
        }
        CursorValue sortValue = position.sortValues().getFirst();
        validate(query.sort(), sortValue);
        return new SeekPosition(sortValue, position.tieBreaker());
    }

    String encode(UUID categoryId, ProductListingQuery query, ProductListingRow lastRow) {
        return codec.encode(context(categoryId, query), List.of(sortValue(query.sort(), lastRow)), lastRow.id());
    }

    private static CursorContext context(UUID categoryId, ProductListingQuery query) {
        String brands = query.brands().stream().distinct().sorted()
            .map(brand -> Base64.getUrlEncoder().withoutPadding().encodeToString(brand.getBytes(StandardCharsets.UTF_8)))
            .collect(Collectors.joining(","));
        return new CursorContext(SCOPE, categoryId.toString(), query.sort(), Map.of(
            "brands", brands,
            "priceFrom", normalizedDecimal(query.priceFrom()),
            "priceTo", normalizedDecimal(query.priceTo()),
            "inStock", query.inStock() == null ? "" : query.inStock().toString()));
    }

    private static String normalizedDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static void validate(String sort, CursorValue value) {
        if (sort.startsWith("price:") && value.type() == CursorValue.Type.NULL) {
            return;
        }
        try {
            switch (sort) {
                case "price:asc", "price:desc" -> value.decimalValue();
                case "createdAt:asc", "createdAt:desc" -> value.instantValue();
                case "popularity:asc", "popularity:desc" -> value.integerValue();
                default -> value.textValue();
            }
        } catch (RuntimeException exception) {
            throw new InvalidCursorException();
        }
    }

    private static CursorValue sortValue(String sort, ProductListingRow row) {
        return switch (sort) {
            case "price:asc", "price:desc" -> CursorValue.decimal(row.priceFrom());
            case "createdAt:asc", "createdAt:desc" -> CursorValue.instant(row.createdAt().toInstant());
            case "popularity:asc", "popularity:desc" -> CursorValue.integer(row.reviewCount());
            default -> CursorValue.text(row.name());
        };
    }

    /** Where the previous page ended: the last row's sort value and id. */
    record SeekPosition(CursorValue sortValue, UUID productId) {
    }
}
