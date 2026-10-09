package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.phuchoang.ecp.catalog.internal.application.browse.query.product.ProductDetailPort;
import org.phuchoang.ecp.catalog.internal.application.browse.query.variant.VariantBrowsePort;
import org.phuchoang.ecp.catalog.internal.application.browse.query.category.CategoryRef;
import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.browse.query.product.ProductDetail;
import org.phuchoang.ecp.catalog.internal.application.browse.query.product.ProductImage;
import org.phuchoang.ecp.catalog.internal.application.browse.query.variant.VariantDetail;
import org.phuchoang.ecp.catalog.internal.application.cart.query.CartVariantQuery;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.JdbcQuerySupport;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Catalog-owned published-product, image, and variant queries. */
@Component
class CatalogProductQueries extends JdbcQuerySupport implements ProductDetailPort, VariantBrowsePort, CartVariantQuery {

  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
  };

  private final ObjectMapper objectMapper;

  CatalogProductQueries(JdbcClient jdbc, ObjectMapper objectMapper) {
    super(jdbc);
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean publishedProductExists(UUID id) {
    return exists("""
        SELECT EXISTS (SELECT 1 FROM catalog_product WHERE id = ? AND publication_status = 'PUBLISHED')
        """, id);
  }

  @Override
  public Optional<ProductDetail> product(UUID id) {
    Optional<ProductDetailRow> product = optional("""
        SELECT p.id, p.name, p.slug, p.description, p.brand, p.publication_status, p.published_at, p.attributes,
               p.average_rating, p.review_count, c.id AS category_id, c.name AS category_name, c.slug AS category_slug
        FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
        WHERE p.id = ? AND p.publication_status = 'PUBLISHED'
        """, this::productDetailRow, id);
    if (product.isEmpty()) {
      return Optional.empty();
    }
    ProductDetailRow row = product.get();
    return Optional.of(new ProductDetail(row.id(), row.name(), row.slug(), row.description(), row.brand(), row.status(),
        row.publishedAt(), List.of(new CategoryRef(row.categoryId(), row.categoryName(), row.categorySlug())),
        objectMap(row.attributes()), images(id), variants(id, Map.of()), row.averageRating(), row.reviewCount()));
  }

  @Override
  public List<VariantDetail> variants(UUID productId, Map<String, String> selected) {
    String sql = selected.isEmpty() ? """
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active, advisory_in_stock
        FROM catalog_variant WHERE product_id = ? ORDER BY sku
        """ : """
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active, advisory_in_stock
        FROM catalog_variant WHERE product_id = ? AND options @> ?::jsonb ORDER BY sku
        """;
    return selected.isEmpty() ? jdbc.sql(sql).param(productId).query(this::variantRow).list()
        : jdbc.sql(sql).params(productId, json(selected)).query(this::variantRow).list();
  }

  @Override
  public Optional<VariantDetail> variant(UUID productId, UUID variantId) {
    return jdbc.sql("""
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active, advisory_in_stock
        FROM catalog_variant WHERE product_id = ? AND id = ?
        """).params(productId, variantId).query(this::variantRow).optional();
  }

  @Override
  public Optional<CurrentVariant> find(UUID variantId) {
    return jdbc.sql("""
        SELECT v.id, v.product_id, v.sku, p.name AS product_name, v.name AS variant_name,
               v.list_price_amount, v.list_price_currency,
               (p.publication_status = 'PUBLISHED' AND v.is_active) AS purchasable,
               CASE WHEN v.advisory_in_stock IS FALSE THEN 0 ELSE NULL END AS available_quantity
        FROM catalog_variant v JOIN catalog_product p ON p.id = v.product_id
        WHERE v.id = ?
        """).param(variantId).query((rs, ignored) -> new CurrentVariant(
            rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class), rs.getString("sku"),
            rs.getString("product_name"), rs.getString("variant_name"),
            new MoneyValue(rs.getBigDecimal("list_price_amount"), rs.getString("list_price_currency")),
            rs.getBoolean("purchasable"), rs.getObject("available_quantity", Integer.class))).optional();
  }

  private ProductDetailRow productDetailRow(ResultSet rs, int ignored) throws SQLException {
    return new ProductDetailRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
        rs.getString("description"), rs.getString("brand"), rs.getString("publication_status"),
        rs.getObject("published_at", OffsetDateTime.class), rs.getString("attributes"),
        decimalAsDouble(rs.getBigDecimal("average_rating")), rs.getInt("review_count"), rs.getObject("category_id", UUID.class),
        rs.getString("category_name"), rs.getString("category_slug"));
  }

  private static Double decimalAsDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private VariantDetail variantRow(ResultSet rs, int ignored) throws SQLException {
    return new VariantDetail(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("name"),
        new MoneyValue(rs.getBigDecimal("list_price_amount"), rs.getString("list_price_currency")), null,
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
    return jdbc.sql("""
        SELECT id, url, alt_text, sort_order FROM catalog_product_image
        WHERE product_id = ? ORDER BY sort_order, id
        """).param(productId).query((rs, ignored) -> new ProductImage(rs.getObject("id", UUID.class),
        rs.getString("url"), rs.getString("alt_text"), rs.getInt("sort_order"))).list();
  }

  private record ProductDetailRow(UUID id, String name, String slug, String description, String brand, String status,
      OffsetDateTime publishedAt, String attributes, Double averageRating, int reviewCount, UUID categoryId,
      String categoryName, String categorySlug) {
  }
}
