package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorValue;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingCursorCodec.SeekPosition;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Composes the category-listing SQL. For name, created-at and popularity browses that do not
 * filter on variant-derived values, the product page is selected before variant and image work.
 * Aggregate-dependent paths still use PostgreSQL for the authoritative calculation, but fetch a
 * cover image only after the 21-row page boundary.
 */
final class ProductListingSqlBuilder {

    static final String PRODUCT_ROWS_CTE = """
        WITH product_rows AS (
            SELECT p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at,
                   MIN(v.list_price_amount) AS price_from, MAX(v.list_price_amount) AS price_to,
                   MIN(v.list_price_currency) AS currency, BOOL_OR(v.advisory_in_stock) AS in_stock
            FROM catalog_product p
            LEFT JOIN catalog_variant v ON v.product_id = p.id AND v.is_active
            JOIN catalog_category c ON c.id = p.category_id
            WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE ? || '%'
            GROUP BY p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at
        )
        """;

    private static final String PAGE_IMAGE_JOIN = """
        LEFT JOIN LATERAL (
            SELECT i.url AS image_url FROM catalog_product_image i
            WHERE i.product_id = page_rows.id
            ORDER BY i.sort_order, i.id LIMIT 1
        ) image ON true
        """;

    private ProductListingSqlBuilder() {
    }

    /** @param path the category's materialised path, which scopes the query to the subtree */
    static ListingSql build(String path, ProductListingQuery query, SeekPosition cursor) {
        return requiresAggregate(query) ? aggregateListing(path, query, cursor) : directListing(path, query, cursor);
    }

    private static ListingSql directListing(String path, ProductListingQuery query, SeekPosition cursor) {
        List<Object> countParameters = new ArrayList<>();
        countParameters.add(path);
        StringBuilder countFilters = new StringBuilder();
        appendBrandFilter(countFilters, countParameters, query, "p.");
        String countSql = """
            SELECT COUNT(*)
            FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
            WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE ? || '%'
            """ + countFilters;

        List<Object> pageParameters = new ArrayList<>();
        pageParameters.add(path);
        StringBuilder pageFilters = new StringBuilder();
        appendBrandFilter(pageFilters, pageParameters, query, "p.");
        if (cursor != null) {
            appendSeekPredicate(pageFilters, pageParameters, query.sort(), cursor, "p.");
        }
        pageParameters.add(query.size() + 1);
        String pageSql = """
            WITH page_rows AS (
                SELECT p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at
                FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
                WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE ? || '%'
            """ + pageFilters + " ORDER BY " + orderBy(query.sort(), "p.") + " LIMIT ?\n)\n"
            + """
            , page_variants AS (
                SELECT v.product_id, MIN(v.list_price_amount) AS price_from, MAX(v.list_price_amount) AS price_to,
                       MIN(v.list_price_currency) AS currency, BOOL_OR(v.advisory_in_stock) AS in_stock
                FROM catalog_variant v JOIN page_rows ON page_rows.id = v.product_id
                WHERE v.is_active GROUP BY v.product_id
            )
            SELECT page_rows.*, page_variants.price_from, page_variants.price_to, page_variants.currency,
                   page_variants.in_stock, image.image_url
            FROM page_rows
            LEFT JOIN page_variants ON page_variants.product_id = page_rows.id
            """ + PAGE_IMAGE_JOIN + " ORDER BY " + orderBy(query.sort(), "page_rows.");
        return new ListingSql(countSql, List.copyOf(countParameters), pageSql, List.copyOf(pageParameters));
    }

    private static ListingSql aggregateListing(String path, ProductListingQuery query, SeekPosition cursor) {
        List<Object> countParameters = new ArrayList<>();
        countParameters.add(path);
        StringBuilder countFilters = new StringBuilder(" WHERE 1 = 1");
        appendFilters(countFilters, countParameters, query, "");
        String countSql = PRODUCT_ROWS_CTE + "SELECT COUNT(*) FROM product_rows" + countFilters;

        List<Object> pageParameters = new ArrayList<>();
        pageParameters.add(path);
        StringBuilder pageFilters = new StringBuilder(" WHERE 1 = 1");
        appendFilters(pageFilters, pageParameters, query, "");
        if (cursor != null) {
            appendSeekPredicate(pageFilters, pageParameters, query.sort(), cursor, "");
        }
        pageParameters.add(query.size() + 1);
        String pageSql = PRODUCT_ROWS_CTE + ", page_rows AS (SELECT * FROM product_rows" + pageFilters + " ORDER BY "
            + orderBy(query.sort()) + " LIMIT ?)\nSELECT page_rows.*, image.image_url FROM page_rows\n"
            + PAGE_IMAGE_JOIN + " ORDER BY " + orderBy(query.sort(), "page_rows.");
        return new ListingSql(countSql, List.copyOf(countParameters), pageSql, List.copyOf(pageParameters));
    }

    private static boolean requiresAggregate(ProductListingQuery query) {
        return query.priceFrom() != null || query.priceTo() != null || query.inStock() != null
            || query.sort().startsWith("price:");
    }

    private static void appendFilters(StringBuilder filters, List<Object> parameters, ProductListingQuery query,
            String prefix) {
        appendBrandFilter(filters, parameters, query, prefix);
        if (query.priceFrom() != null) {
            filters.append(" AND ").append(prefix).append("price_from >= ?");
            parameters.add(query.priceFrom());
        }
        if (query.priceTo() != null) {
            filters.append(" AND ").append(prefix).append("price_from <= ?");
            parameters.add(query.priceTo());
        }
        if (query.inStock() != null) {
            filters.append(" AND ").append(prefix).append("in_stock = ?");
            parameters.add(query.inStock());
        }
    }

    private static void appendBrandFilter(StringBuilder filters, List<Object> parameters, ProductListingQuery query,
            String prefix) {
        if (!query.brands().isEmpty()) {
            filters.append(" AND ").append(prefix).append("brand IN (")
                .append("?, ".repeat(query.brands().size() - 1)).append("?)");
            parameters.addAll(query.brands());
        }
    }

    static String orderBy(String sort) {
        return orderBy(sort, "");
    }

    private static String orderBy(String sort, String prefix) {
        return switch (sort) {
            case "price:asc" -> prefix + "price_from ASC NULLS LAST, " + prefix + "id ASC";
            case "price:desc" -> prefix + "price_from DESC NULLS LAST, " + prefix + "id ASC";
            case "createdAt:asc" -> prefix + "created_at ASC, " + prefix + "id ASC";
            case "createdAt:desc" -> prefix + "created_at DESC, " + prefix + "id ASC";
            case "popularity:asc" -> prefix + "review_count ASC, " + prefix + "id ASC";
            case "popularity:desc" -> prefix + "review_count DESC, " + prefix + "id ASC";
            default -> prefix + "name ASC, " + prefix + "id ASC";
        };
    }

    private static void appendSeekPredicate(StringBuilder filters, List<Object> parameters, String sort,
            SeekPosition cursor, String prefix) {
        UUID lastId = cursor.productId();
        if (sort.startsWith("price:")) {
            BigDecimal lastPrice = cursor.sortValue().type() == CursorValue.Type.NULL ? null : cursor.sortValue().decimalValue();
            if (lastPrice == null) {
                filters.append(" AND (").append(prefix).append("price_from IS NULL AND ").append(prefix).append("id > ?)");
                parameters.add(lastId);
            } else {
                String comparison = sort.endsWith(":desc") ? "<" : ">";
                filters.append(" AND (").append(prefix).append("price_from IS NULL OR ").append(prefix).append("price_from ")
                    .append(comparison).append(" ? OR (").append(prefix).append("price_from = ? AND ")
                    .append(prefix).append("id > ?))");
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
        filters.append(" AND (").append(prefix).append(column).append(" ").append(comparison)
            .append(" ? OR (").append(prefix).append(column).append(" = ? AND ").append(prefix).append("id > ?))");
        parameters.add(lastValue);
        parameters.add(lastValue);
        parameters.add(lastId);
    }

    /** The two statements a page needs and their positional parameters. */
    record ListingSql(String countSql, List<Object> countParameters, String pageSql, List<Object> pageParameters) {
    }
}
