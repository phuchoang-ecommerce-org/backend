package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.phuchoang.ecp.catalog.internal.application.query.model.listing.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingCursorCodec.SeekPosition;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Composes the category-listing SQL: the {@code product_rows} CTE (variant-derived price range,
 * stock advisory and cover image aggregated <em>before</em> filtering and keyset pagination), the
 * filter predicates, the seek predicate for a cursor, and the sort order. Pure string/parameter
 * assembly — nothing here touches a connection.
 */
final class ProductListingSqlBuilder {

    static final String PRODUCT_ROWS_CTE = """
        WITH product_rows AS (
            SELECT p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at,
                   MIN(v.list_price_amount) AS price_from, MAX(v.list_price_amount) AS price_to,
                   MIN(v.list_price_currency) AS currency, BOOL_OR(v.advisory_in_stock) AS in_stock,
                   (SELECT i.url FROM catalog_product_image i WHERE i.product_id = p.id ORDER BY i.sort_order, i.id LIMIT 1) AS image_url
            FROM catalog_product p
            LEFT JOIN catalog_variant v ON v.product_id = p.id AND v.is_active
            JOIN catalog_category c ON c.id = p.category_id
            WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE ? || '%'
            GROUP BY p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at
        )
        """;

    private ProductListingSqlBuilder() {
    }

    /** @param path the category's materialised path, which scopes the CTE to the subtree */
    static ListingSql build(String path, ProductListingQuery query, SeekPosition cursor) {
        List<Object> parameters = new ArrayList<>();
        parameters.add(path);
        StringBuilder filters = new StringBuilder(" WHERE 1 = 1");
        if (!query.brands().isEmpty()) {
            filters.append(" AND brand IN (").append("?, ".repeat(query.brands().size() - 1)).append("?)");
            parameters.addAll(query.brands());
        }
        if (query.priceFrom() != null) {
            filters.append(" AND price_from >= ?");
            parameters.add(query.priceFrom());
        }
        if (query.priceTo() != null) {
            filters.append(" AND price_from <= ?");
            parameters.add(query.priceTo());
        }
        if (query.inStock() != null) {
            filters.append(" AND in_stock = ?");
            parameters.add(query.inStock());
        }
        String countSql = PRODUCT_ROWS_CTE + "SELECT COUNT(*) FROM product_rows" + filters;
        List<Object> countParameters = List.copyOf(parameters);

        if (cursor != null) {
            appendSeekPredicate(filters, parameters, query.sort(), cursor);
        }
        parameters.add(query.size() + 1); // one look-ahead row decides whether a next page exists
        String pageSql = PRODUCT_ROWS_CTE + "SELECT * FROM product_rows" + filters + " ORDER BY " + orderBy(query.sort())
            + " LIMIT ?";
        return new ListingSql(countSql, countParameters, pageSql, List.copyOf(parameters));
    }

    static String orderBy(String sort) {
        return switch (sort) {
            case "price:asc" -> "price_from ASC NULLS LAST, id ASC";
            case "price:desc" -> "price_from DESC NULLS LAST, id ASC";
            case "createdAt:asc" -> "created_at ASC, id ASC";
            case "createdAt:desc" -> "created_at DESC, id ASC";
            case "popularity:asc" -> "review_count ASC, id ASC";
            case "popularity:desc" -> "review_count DESC, id ASC";
            default -> "name ASC, id ASC";
        };
    }

    private static void appendSeekPredicate(StringBuilder filters, List<Object> parameters, String sort,
            SeekPosition cursor) {
        UUID lastId = cursor.productId();
        if (sort.startsWith("price:")) {
            BigDecimal lastPrice = cursor.sortValue().type() == CursorValue.Type.NULL ? null : cursor.sortValue().decimalValue();
            if (lastPrice == null) {
                filters.append(" AND (price_from IS NULL AND id > ?)");
                parameters.add(lastId);
            } else {
                String comparison = sort.endsWith(":desc") ? "<" : ">";
                filters.append(" AND (price_from IS NULL OR price_from ").append(comparison)
                    .append(" ? OR (price_from = ? AND id > ?))");
                parameters.add(lastPrice);
                parameters.add(lastPrice);
                parameters.add(lastId);
            }
            return;
        }
        Object lastValue = switch (sort) {
            case "createdAt:asc", "createdAt:desc" -> OffsetDateTime.ofInstant(cursor.sortValue().instantValue(), ZoneOffset.UTC);
            case "popularity:asc", "popularity:desc" -> cursor.sortValue().integerValue();
            default -> cursor.sortValue().textValue();
        };
        String column = switch (sort) {
            case "createdAt:asc", "createdAt:desc" -> "created_at";
            case "popularity:asc", "popularity:desc" -> "review_count";
            default -> "name";
        };
        String comparison = sort.endsWith(":desc") ? "<" : ">";
        filters.append(" AND (").append(column).append(" ").append(comparison)
            .append(" ? OR (").append(column).append(" = ? AND id > ?))");
        parameters.add(lastValue);
        parameters.add(lastValue);
        parameters.add(lastId);
    }

    /** The two statements a page needs and their positional parameters. */
    record ListingSql(String countSql, List<Object> countParameters, String pageSql, List<Object> pageParameters) {
    }
}
