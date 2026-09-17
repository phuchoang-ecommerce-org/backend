package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel.ListingQuery;
import org.phuchoang.ecp.catalog.internal.application.query.CatalogBrowseModel.ProductPage;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorSigningKey;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.HmacCursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogProductListingQueriesTest {

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void appliesFiltersSortAndSeekCursorInDatabaseQuery() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<String> pathResult = mock(JdbcClient.MappedQuerySpec.class);
    JdbcClient.MappedQuerySpec<Long> countResult = mock(JdbcClient.MappedQuerySpec.class);
    JdbcClient.MappedQuerySpec<Object> rowsResult = mock(JdbcClient.MappedQuerySpec.class);
    UUID categoryId = UUID.randomUUID();
    UUID lastProductId = UUID.randomUUID();
    ListingQuery withoutCursor = new ListingQuery(null, 2, "price:desc", List.of("Acme", "Contoso"),
        new BigDecimal("10.00"), new BigDecimal("50.00"), null);
    ListingQuery query = new ListingQuery(cursor(categoryId, withoutCursor, "42.00", lastProductId), 2, "price:desc",
        List.of("Acme", "Contoso"), new BigDecimal("10.00"), new BigDecimal("50.00"), null);
    when(jdbc.sql(any(String.class))).thenReturn(statement);
    when(statement.param(categoryId)).thenReturn(statement);
    when(statement.params(any(Object[].class))).thenReturn(statement);
    when(statement.params(any(List.class))).thenReturn(statement);
    when(statement.query(String.class)).thenReturn(pathResult);
    when(statement.query(Long.class)).thenReturn(countResult);
    when(statement.query(any(RowMapper.class))).thenReturn((JdbcClient.MappedQuerySpec) rowsResult);
    when(pathResult.single()).thenReturn("/electronics/");
    when(countResult.single()).thenReturn(5L);
    when(rowsResult.list()).thenReturn(List.of());

    ProductPage page = new CatalogProductListingQueries(jdbc, codec()).products(categoryId, query);

    assertThat(page.total()).isEqualTo(5);
    assertThat(page.nextCursor()).isNull();
    verify(jdbc).sql(argThat(sql -> sql.contains("brand IN (?, ?)")
            && sql.contains("price_from >= ?") && sql.contains("price_from <= ?")
            && sql.contains("price_from IS NULL OR price_from < ? OR (price_from = ? AND id > ?)")
            && sql.contains("ORDER BY price_from DESC NULLS LAST, id ASC LIMIT ?") && !sql.contains("OFFSET")));
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void rejectsMalformedCursorBeforeQueryingProducts() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<String> pathResult = mock(JdbcClient.MappedQuerySpec.class);
    UUID categoryId = UUID.randomUUID();
    when(jdbc.sql(any(String.class))).thenReturn(statement);
    when(statement.param(categoryId)).thenReturn(statement);
    when(statement.params(any(Object[].class))).thenReturn(statement);
    when(statement.query(String.class)).thenReturn(pathResult);
    when(pathResult.single()).thenReturn("/electronics/");

    assertThatThrownBy(() -> new CatalogProductListingQueries(jdbc, codec()).products(categoryId,
        new ListingQuery("not-a-cursor", 20, "default", List.of(), null, null, null)))
        .isInstanceOf(InvalidCursorException.class)
        .hasMessage("Cursor is malformed or incompatible with this request.");
  }

  private static String cursor(UUID categoryId, ListingQuery query, String sortValue, UUID productId) {
    return codec().encode(context(categoryId, query), List.of(CursorValue.decimal(new BigDecimal(sortValue))), productId);
  }

  private static CursorContext context(UUID categoryId, ListingQuery query) {
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

  private static CursorCodec codec() {
    return new HmacCursorCodec(CursorSigningKey.utf8("test-key", "01234567890123456789012345678901"), null);
  }
}
