package org.phuchoang.ecp.catalog.infrastructure.persistence.browse;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.CategoryRef;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Money;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ProductDetail;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ProductImage;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Variant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Catalog-owned published-product, image, and variant queries. */
@Component
class CatalogProductQueries {

  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
  };

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  CatalogProductQueries(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  boolean publishedExists(UUID id) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS (SELECT 1 FROM catalog_product WHERE id = ? AND publication_status = 'PUBLISHED')
        """, Boolean.class, id));
  }

  Optional<ProductDetail> product(UUID id) {
    List<ProductDetailRow> products = jdbc.query("""
        SELECT p.id, p.name, p.slug, p.description, p.brand, p.publication_status, p.published_at, p.attributes,
               p.average_rating, p.review_count, c.id AS category_id, c.name AS category_name, c.slug AS category_slug
        FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
        WHERE p.id = ? AND p.publication_status = 'PUBLISHED'
        """, this::productDetailRow, id);
    if (products.isEmpty()) {
      return Optional.empty();
    }
    ProductDetailRow row = products.getFirst();
    return Optional.of(new ProductDetail(row.id(), row.name(), row.slug(), row.description(), row.brand(), row.status(),
        row.publishedAt(), List.of(new CategoryRef(row.categoryId(), row.categoryName(), row.categorySlug())),
        objectMap(row.attributes()), images(id), variants(id, Map.of()), row.averageRating(), row.reviewCount()));
  }

  List<Variant> variants(UUID productId, Map<String, String> selected) {
    String sql = selected.isEmpty() ? """
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active, advisory_in_stock
        FROM catalog_variant WHERE product_id = ? ORDER BY sku
        """ : """
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active, advisory_in_stock
        FROM catalog_variant WHERE product_id = ? AND options @> ?::jsonb ORDER BY sku
        """;
    return selected.isEmpty() ? jdbc.query(sql, this::variantRow, productId)
        : jdbc.query(sql, this::variantRow, productId, json(selected));
  }

  Optional<Variant> variant(UUID productId, UUID variantId) {
    return jdbc.query("""
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active, advisory_in_stock
        FROM catalog_variant WHERE product_id = ? AND id = ?
        """, this::variantRow, productId, variantId).stream().findFirst();
  }

  private ProductDetailRow productDetailRow(ResultSet rs, int ignored) throws SQLException {
    return new ProductDetailRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
        rs.getString("description"), rs.getString("brand"), rs.getString("publication_status"),
        rs.getObject("published_at", OffsetDateTime.class), rs.getString("attributes"),
        (Double) rs.getObject("average_rating"), rs.getInt("review_count"), rs.getObject("category_id", UUID.class),
        rs.getString("category_name"), rs.getString("category_slug"));
  }

  private Variant variantRow(ResultSet rs, int ignored) throws SQLException {
    return new Variant(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("name"),
        new Money(rs.getBigDecimal("list_price_amount"), rs.getString("list_price_currency")), null,
        stringMap(rs.getString("options")), rs.getObject("weight_grams", Integer.class), rs.getBoolean("is_active"),
        rs.getObject("advisory_in_stock", Boolean.class));
  }

  private Map<String, String> stringMap(String json) {
    try {
      return objectMapper.readValue(json, STRING_MAP);
    } catch (Exception exception) {
      throw new IllegalStateException("catalog_variant options is not a string map", exception);
    }
  }

  private Map<String, Object> objectMap(String json) {
    try {
      return objectMapper.readValue(json, OBJECT_MAP);
    } catch (Exception exception) {
      throw new IllegalStateException("catalog_product attributes is not an object", exception);
    }
  }

  private String json(Map<String, String> values) {
    try {
      return objectMapper.writeValueAsString(values);
    } catch (Exception exception) {
      throw new IllegalStateException("catalog variant option filter cannot be serialized", exception);
    }
  }

  private List<ProductImage> images(UUID productId) {
    return jdbc.query("""
        SELECT id, url, alt_text, sort_order FROM catalog_product_image
        WHERE product_id = ? ORDER BY sort_order, id
        """, (rs, ignored) -> new ProductImage(rs.getObject("id", UUID.class), rs.getString("url"),
        rs.getString("alt_text"), rs.getInt("sort_order")), productId);
  }

  private record ProductDetailRow(UUID id, String name, String slug, String description, String brand, String status,
      OffsetDateTime publishedAt, String attributes, Double averageRating, int reviewCount, UUID categoryId,
      String categoryName, String categorySlug) {
  }
}
