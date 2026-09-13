package org.phuchoang.ecp.catalog.infrastructure.persistence;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.phuchoang.ecp.catalog.application.port.CatalogBrowsePort;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Category;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.CategoryNode;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.CategoryRef;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ListingQuery;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Money;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ProductPage;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.ProductSummary;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Variant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL read adapter. Reads only the catalog-owned tables; inventory
 * remains advisory until its sprint.
 */
@Repository
public class CatalogBrowseRepository implements CatalogBrowsePort {

  /** Jackson type token for a variant's string-valued option map. */
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
  };

  /** JDBC access to catalog-owned PostgreSQL tables. */
  private final JdbcTemplate jdbc;
  /** JSON mapper used to materialize variant option maps. */
  private final ObjectMapper objectMapper;

  /**
   * Creates the PostgreSQL browse adapter.
   *
   * @param jdbc JDBC template for catalog queries
   * @param objectMapper mapper for JSON option data
   */
  public CatalogBrowseRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
  }

  /** {@inheritDoc} */
  @Override
  public boolean categoryExists(UUID id) {
    return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM catalog_category WHERE id = ?)",
        Boolean.class, id));
  }

  /** {@inheritDoc} */
  @Override
  public boolean publishedProductExists(UUID id) {
    return Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS (SELECT 1 FROM catalog_product WHERE id = ? AND publication_status = 'PUBLISHED')
        """, Boolean.class, id));
  }

  /** {@inheritDoc} */
  @Override
  public Optional<Category> category(UUID id) {
    List<CategoryRow> rows = jdbc.query("""
        SELECT id, parent_id, name, slug, path, depth, sort_order
        FROM catalog_category WHERE id = ?
        """, this::categoryRow, id);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    CategoryRow row = rows.getFirst();
    return Optional.of(toView(row, ancestors(row)));
  }

  /** {@inheritDoc} */
  @Override
  public List<CategoryNode> wholeTree() {
    return tree(null, null);
  }

  /** {@inheritDoc} */
  @Override
  public List<CategoryNode> tree(UUID rootId, Integer maxDepth) {
    String sql = rootId == null ? """
        SELECT id, parent_id, name, slug, path, depth, sort_order FROM catalog_category ORDER BY depth, sort_order, name
        """ : """
        SELECT child.id, child.parent_id, child.name, child.slug, child.path, child.depth, child.sort_order
        FROM catalog_category child JOIN catalog_category root ON root.id = ?
        WHERE child.path LIKE root.path || '%' ORDER BY child.depth, child.sort_order, child.name
        """;
    List<CategoryRow> rows = rootId == null ? jdbc.query(sql, this::categoryRow)
        : jdbc.query(sql, this::categoryRow, rootId);
    if (maxDepth != null) {
      int base = rootId == null ? 0
          : rows.stream().filter(row -> row.id.equals(rootId)).findFirst().map(row -> row.depth).orElse(0);
      rows = rows.stream().filter(row -> row.depth <= base + maxDepth).toList();
    }
    Map<UUID, MutableNode> nodes = new LinkedHashMap<>();
    for (CategoryRow row : rows) {
      nodes.put(row.id, new MutableNode(row, rootId == null ? List.of() : ancestors(row)));
    }
    List<MutableNode> roots = new ArrayList<>();
    for (MutableNode node : nodes.values()) {
      MutableNode parent = nodes.get(node.row.parentId);
      if (parent == null || node.row.id.equals(rootId)) {
        roots.add(node);
      } else {
        parent.children.add(node);
      }
    }
    return roots.stream().map(MutableNode::freeze).toList();
  }

  /** {@inheritDoc} */
  @Override
  public ProductPage products(UUID categoryId, ListingQuery query) {
    String path = jdbc.queryForObject("SELECT path FROM catalog_category WHERE id = ?", String.class, categoryId);
    List<ProductRow> candidates = jdbc.query(
        """
            SELECT p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at,
                   MIN(v.list_price_amount) AS price_from, MAX(v.list_price_amount) AS price_to,
                   MIN(v.list_price_currency) AS currency,
                   (SELECT i.url FROM catalog_product_image i WHERE i.product_id = p.id ORDER BY i.sort_order, i.id LIMIT 1) AS image_url
            FROM catalog_product p
            LEFT JOIN catalog_variant v ON v.product_id = p.id AND v.is_active
            JOIN catalog_category c ON c.id = p.category_id
            WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE ? || '%'
            GROUP BY p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at
            """,
        this::productRow, path);
    candidates = candidates.stream().filter(row -> matches(row, query)).sorted(order(query.sort())).toList();
    int start = offset(query.cursor(), query.sort());
    if (start >= candidates.size() && !candidates.isEmpty()) {
      start = Math.max(0, candidates.size() - query.size());
    }
    int end = Math.min(start + query.size(), candidates.size());
    List<ProductSummary> items = candidates.subList(start, end).stream().map(this::productView).toList();
    String next = end < candidates.size() ? encodeCursor(query.sort() + "|" + end) : null;
    return new ProductPage(items, next, candidates.size());
  }

  /** {@inheritDoc} */
  @Override
  public List<Variant> variants(UUID productId, Map<String, String> selected) {
    return jdbc.query("""
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active
        FROM catalog_variant WHERE product_id = ? ORDER BY sku
        """, this::variantRow, productId).stream()
        .filter(variant -> variant.options().entrySet().containsAll(selected.entrySet())).toList();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<Variant> variant(UUID productId, UUID variantId) {
    List<Variant> variants = jdbc.query("""
        SELECT id, sku, name, list_price_amount, list_price_currency, options, weight_grams, is_active
        FROM catalog_variant WHERE product_id = ? AND id = ?
        """, this::variantRow, productId, variantId);
    return variants.stream().findFirst();
  }

  /**
   * Applies in-memory listing filters after the descendant query. Inventory is intentionally not
   * joined: until the inventory module exists, availability is represented as unknown rather than
   * suppressing a browse result.
   *
   * @param row candidate product row
   * @param query normalized listing filters
   * @return {@code true} when the row satisfies the currently implementable filters
   */
  private boolean matches(ProductRow row, ListingQuery query) {
    if (!query.brands().isEmpty() && (row.brand == null || !query.brands().contains(row.brand)))
      return false;
    if (query.priceFrom() != null && (row.priceFrom == null || row.priceFrom.compareTo(query.priceFrom()) < 0))
      return false;
    if (query.priceTo() != null && (row.priceFrom == null || row.priceFrom.compareTo(query.priceTo()) > 0))
      return false;
    // Inventory is intentionally not joined: until Sprint 11 the only honest browse
    // state is unknown.
    // An unavailable inventory dependency must not suppress the listing (UC-CAT-02
    // E3).
    return true;
  }

  /**
   * Builds the comparator for a supported listing sort expression.
   *
   * @param sort normalized sort expression
   * @return comparator with name-and-identifier ordering as the default
   */
  private static Comparator<ProductRow> order(String sort) {
    return switch (sort) {
      case "price:asc" -> Comparator.comparing(row -> row.priceFrom, Comparator.nullsLast(Comparator.naturalOrder()));
      case "price:desc" -> Comparator.comparing((ProductRow row) -> row.priceFrom,
          Comparator.nullsLast(Comparator.reverseOrder()));
      case "createdAt:asc" -> Comparator.comparing(row -> row.createdAt);
      case "createdAt:desc" -> Comparator.comparing((ProductRow row) -> row.createdAt).reversed();
      case "popularity:asc" -> Comparator.comparingInt(row -> row.reviewCount);
      case "popularity:desc" -> Comparator.comparingInt((ProductRow row) -> row.reviewCount).reversed();
      default -> Comparator.comparing((ProductRow row) -> row.name).thenComparing(row -> row.id);
    };
  }

  /**
   * Decodes the offset cursor only when it belongs to the requested sort order.
   *
   * @param cursor opaque Base64 cursor, or {@code null}
   * @param sort current normalized sort expression
   * @return non-negative list offset, or zero for an absent, invalid, or mismatched cursor
   */
  private static int offset(String cursor, String sort) {
    if (cursor == null || cursor.isBlank())
      return 0;
    try {
      String[] parts = decodeCursor(cursor).split("\\|", 2);
      return parts.length == 2 && parts[0].equals(sort) ? Math.max(0, Integer.parseInt(parts[1])) : 0;
    } catch (IllegalArgumentException ignored) {
      return 0;
    }
  }

  /**
   * Encodes the repository's internal cursor payload as URL-safe Base64.
   *
   * @param raw sort and offset payload
   * @return opaque cursor value
   */
  private static String encodeCursor(String raw) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * Decodes a URL-safe Base64 cursor payload.
   *
   * @param cursor opaque cursor value
   * @return decoded payload
   * @throws IllegalArgumentException when the cursor is not valid Base64
   */
  private static String decodeCursor(String cursor) {
    return new String(Base64.getUrlDecoder().decode(cursor), java.nio.charset.StandardCharsets.UTF_8);
  }

  /**
   * Maps one category query result row.
   *
   * @param rs JDBC result set positioned on a category row
   * @param ignored row number unused by this mapper
   * @return compact category row
   * @throws SQLException when JDBC cannot read a column
   */
  private CategoryRow categoryRow(ResultSet rs, int ignored) throws SQLException {
    return new CategoryRow(rs.getObject("id", UUID.class), rs.getObject("parent_id", UUID.class), rs.getString("name"),
        rs.getString("slug"), rs.getString("path"), rs.getInt("depth"), rs.getInt("sort_order"));
  }

  /**
   * Maps one product-listing query result row.
   *
   * @param rs JDBC result set positioned on a product row
   * @param ignored row number unused by this mapper
   * @return compact product row
   * @throws SQLException when JDBC cannot read a column
   */
  private ProductRow productRow(ResultSet rs, int ignored) throws SQLException {
    return new ProductRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
        rs.getString("brand"),
        rs.getString("publication_status"), rs.getString("image_url"), rs.getBigDecimal("price_from"),
        rs.getBigDecimal("price_to"),
        rs.getString("currency"), (Double) rs.getObject("average_rating"), rs.getInt("review_count"),
        rs.getObject("created_at", OffsetDateTime.class));
  }

  /**
   * Maps one variant query result row and decodes its option map.
   *
   * @param rs JDBC result set positioned on a variant row
   * @param ignored row number unused by this mapper
   * @return internal variant projection with unknown inventory state
   * @throws SQLException when JDBC cannot read a column
   */
  private Variant variantRow(ResultSet rs, int ignored) throws SQLException {
    return new Variant(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("name"),
        new Money(rs.getBigDecimal("list_price_amount"), rs.getString("list_price_currency")),
        stringMap(rs.getString("options")), rs.getObject("weight_grams", Integer.class), rs.getBoolean("is_active"),
        null);
  }

  /**
   * Parses the catalog JSON options column as a string-to-string map.
   *
   * @param json serialized option dimensions and values
   * @return parsed option map
   * @throws IllegalStateException when the stored JSON does not have the expected shape
   */
  private Map<String, String> stringMap(String json) {
    try {
      return objectMapper.readValue(json, STRING_MAP);
    } catch (Exception exception) {
      throw new IllegalStateException("catalog_variant options is not a string map", exception);
    }
  }

  /**
   * Converts a compact persistence category row into an internal direct-lookup projection.
   *
   * @param row category persistence row
   * @param ancestors resolved root-to-parent references
   * @return internal category projection
   */
  private Category toView(CategoryRow row, List<CategoryRef> ancestors) {
    return new Category(row.id, row.parentId, row.name, row.slug, row.depth, row.sortOrder, null, false, ancestors);
  }

  /**
   * Resolves the root-to-parent breadcrumb chain from a materialized category path.
   *
   * @param row category for which ancestors are required
   * @return ordered ancestor references, or an empty list for a root category
   */
  private List<CategoryRef> ancestors(CategoryRow row) {
    if (row.parentId == null)
      return List.of();
    return jdbc.query("""
        SELECT id, name, slug FROM catalog_category
        WHERE ? LIKE path || '%' AND id <> ? ORDER BY depth
        """,
        (rs, ignored) -> new CategoryRef(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug")),
        row.path, row.id);
  }

  /**
   * Converts a persistence product row into its listing projection.
   *
   * @param row product persistence row
   * @return product summary with advisory availability left unknown
   */
  private ProductSummary productView(ProductRow row) {
    Money from = row.priceFrom == null ? null : new Money(row.priceFrom, row.currency);
    Money to = row.priceTo == null ? null : new Money(row.priceTo, row.currency);
    return new ProductSummary(row.id, row.name, row.slug, row.brand, row.status, row.imageUrl, from, to,
        row.averageRating, row.reviewCount, null);
  }

  /**
   * Columns required to construct category tree and breadcrumb projections.
   *
   * @param id category identifier
   * @param parentId parent identifier, or {@code null} for a root
   * @param name display name
   * @param slug human-readable category identifier
   * @param path materialized slash-delimited hierarchy path
   * @param depth zero-based tree depth
   * @param sortOrder configured sibling order
   */
  private record CategoryRow(UUID id, UUID parentId, String name, String slug, String path, int depth, int sortOrder) {
  }

  /**
   * Columns required to filter, sort, and project a category listing result.
   *
   * @param id product identifier
   * @param name display name
   * @param slug human-readable product identifier
   * @param brand optional brand
   * @param status publication status
   * @param imageUrl optional primary image URL
   * @param priceFrom lowest active-variant price
   * @param priceTo highest active-variant price
   * @param currency currency associated with aggregated prices
   * @param averageRating denormalized review average
   * @param reviewCount number of reviews
   * @param createdAt product creation time used by newest sorts
   */
  private record ProductRow(UUID id, String name, String slug, String brand, String status, String imageUrl,
      BigDecimal priceFrom, BigDecimal priceTo, String currency, Double averageRating,
      int reviewCount, OffsetDateTime createdAt) {
  }

  /** Mutable intermediate node used to assemble an ordered recursive category tree. */
  private static final class MutableNode {
    /** Persistence data for this tree node. */
    private final CategoryRow row;
    /** Breadcrumb references resolved for subtree reads. */
    private final List<CategoryRef> ancestors;
    /** Children accumulated before the immutable projection is created. */
    private final List<MutableNode> children = new ArrayList<>();

    /**
     * Creates an intermediate tree node.
     *
     * @param row category persistence row
     * @param ancestors resolved breadcrumb chain
     */
    private MutableNode(CategoryRow row, List<CategoryRef> ancestors) {
      this.row = row;
      this.ancestors = ancestors;
    }

    /**
     * Recursively converts this mutable structure into the immutable read model.
     *
     * @return category navigation node with frozen descendants
     */
    private CategoryNode freeze() {
      return new CategoryNode(row.id, row.parentId, row.name, row.slug, row.depth, row.sortOrder, null, false,
          ancestors, children.stream().map(MutableNode::freeze).toList());
    }
  }
}
