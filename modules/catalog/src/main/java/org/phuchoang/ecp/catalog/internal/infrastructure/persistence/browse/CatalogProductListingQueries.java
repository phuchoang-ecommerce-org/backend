package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse;

import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel.ListingQuery;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel.Money;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel.ProductPage;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel.ProductSummary;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.JdbcQuerySupport;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorPosition;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Catalog category-product listing queries, including keyset cursor handling. */
@Component
class CatalogProductListingQueries extends JdbcQuerySupport {

  private static final String PRODUCT_ROWS_CTE = """
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

  private final CursorCodec cursorCodec;

  CatalogProductListingQueries(JdbcClient jdbc, CursorCodec cursorCodec) {
    super(jdbc);
    this.cursorCodec = cursorCodec;
  }

  ProductPage products(UUID categoryId, ListingQuery query) {
    String path = required("SELECT path FROM catalog_category WHERE id = ?", String.class, categoryId);
    ProductListingSql listing = productListingSql(path, categoryId, query);
    long total = jdbc.sql(PRODUCT_ROWS_CTE + "SELECT COUNT(*) FROM product_rows" + listing.countFilters())
        .params(listing.countParameters()).query(Long.class).single();
    List<Object> pageParameters = new ArrayList<>(listing.pageParameters());
    pageParameters.add(query.size() + 1);
    List<ProductRow> rows = jdbc.sql(PRODUCT_ROWS_CTE + "SELECT * FROM product_rows" + listing.pageFilters()
            + " ORDER BY " + listing.orderBy() + " LIMIT ?").params(pageParameters).query(this::productRow).list();
    boolean hasMore = rows.size() > query.size();
    if (hasMore) {
      rows = rows.subList(0, query.size());
    }
    String next = hasMore ? encodeCursor(categoryId, query, rows.getLast()) : null;
    return new ProductPage(rows.stream().map(this::productView).toList(), next, total);
  }

  private ProductListingSql productListingSql(String path, UUID categoryId, ListingQuery query) {
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
    String countFilters = filters.toString();
    List<Object> countParameters = List.copyOf(parameters);
    Cursor cursor = decodeCursor(categoryId, query);
    if (cursor != null) {
      appendSeekPredicate(filters, parameters, query.sort(), cursor);
    }
    return new ProductListingSql(countFilters, countParameters, filters.toString(), List.copyOf(parameters), orderBy(query.sort()));
  }

  private static String orderBy(String sort) {
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

  private static void appendSeekPredicate(StringBuilder filters, List<Object> parameters, String sort, Cursor cursor) {
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

  private String encodeCursor(UUID categoryId, ListingQuery query, ProductRow row) {
    return cursorCodec.encode(cursorContext(categoryId, query), List.of(sortValue(query.sort(), row)), row.id());
  }

  private Cursor decodeCursor(UUID categoryId, ListingQuery query) {
    if (query.cursor() == null || query.cursor().isBlank()) {
      return null;
    }
    CursorPosition position = cursorCodec.decode(query.cursor(), cursorContext(categoryId, query));
    if (position.sortValues().size() != 1) {
      throw new InvalidCursorException();
    }
    CursorValue sortValue = position.sortValues().getFirst();
    validateSortValue(query.sort(), sortValue);
    return new Cursor(sortValue, position.tieBreaker());
  }

  private static CursorContext cursorContext(UUID categoryId, ListingQuery query) {
    String brands = query.brands().stream().distinct().sorted()
        .map(brand -> java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(brand.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
        .collect(java.util.stream.Collectors.joining(","));
    return new CursorContext("catalog.category-products", categoryId.toString(), query.sort(), Map.of(
        "brands", brands,
        "priceFrom", normalizedDecimal(query.priceFrom()),
        "priceTo", normalizedDecimal(query.priceTo()),
        "inStock", query.inStock() == null ? "" : query.inStock().toString()));
  }

  private static String normalizedDecimal(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  private static void validateSortValue(String sort, CursorValue value) {
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

  private static CursorValue sortValue(String sort, ProductRow row) {
    return switch (sort) {
      case "price:asc", "price:desc" -> CursorValue.decimal(row.priceFrom());
      case "createdAt:asc", "createdAt:desc" -> CursorValue.instant(row.createdAt().toInstant());
      case "popularity:asc", "popularity:desc" -> CursorValue.integer(row.reviewCount());
      default -> CursorValue.text(row.name());
    };
  }

  private ProductRow productRow(ResultSet rs, int ignored) throws SQLException {
    return new ProductRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
        rs.getString("brand"), rs.getString("publication_status"), rs.getString("image_url"),
        rs.getBigDecimal("price_from"), rs.getBigDecimal("price_to"), rs.getString("currency"),
        (Double) rs.getObject("average_rating"), rs.getInt("review_count"),
        rs.getObject("in_stock", Boolean.class), rs.getObject("created_at", OffsetDateTime.class));
  }

  private ProductSummary productView(ProductRow row) {
    Money from = row.priceFrom == null ? null : new Money(row.priceFrom, row.currency);
    Money to = row.priceTo == null ? null : new Money(row.priceTo, row.currency);
    return new ProductSummary(row.id, row.name, row.slug, row.brand, row.status, row.imageUrl, from, to,
        row.averageRating, row.reviewCount, row.inStock);
  }

  private record ProductRow(UUID id, String name, String slug, String brand, String status, String imageUrl,
      BigDecimal priceFrom, BigDecimal priceTo, String currency, Double averageRating,
      int reviewCount, Boolean inStock, OffsetDateTime createdAt) {
  }

  private record ProductListingSql(String countFilters, List<Object> countParameters, String pageFilters,
      List<Object> pageParameters, String orderBy) {
  }

  private record Cursor(CursorValue sortValue, UUID productId) {
  }
}
