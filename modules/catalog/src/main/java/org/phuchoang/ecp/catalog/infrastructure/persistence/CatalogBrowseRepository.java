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
import org.phuchoang.ecp.sharedkernel.api.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.CursorPosition;
import org.phuchoang.ecp.sharedkernel.api.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.InvalidCursorException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

  /** Aggregates the variant-dependent fields once before filtering and paging the listing. */
  private static final String PRODUCT_ROWS_CTE = """
      WITH product_rows AS (
          SELECT p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at,
                 MIN(v.list_price_amount) AS price_from, MAX(v.list_price_amount) AS price_to,
                 MIN(v.list_price_currency) AS currency,
                 (SELECT i.url FROM catalog_product_image i WHERE i.product_id = p.id ORDER BY i.sort_order, i.id LIMIT 1) AS image_url
          FROM catalog_product p
          LEFT JOIN catalog_variant v ON v.product_id = p.id AND v.is_active
          JOIN catalog_category c ON c.id = p.category_id
          WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE ? || '%'
          GROUP BY p.id, p.name, p.slug, p.brand, p.publication_status, p.average_rating, p.review_count, p.created_at
      )
      """;

  /** Jackson type token for a variant's string-valued option map. */
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
  };

  /** JDBC access to catalog-owned PostgreSQL tables. */
  private final JdbcTemplate jdbc;
  /** JSON mapper used to materialize variant option maps. */
  private final ObjectMapper objectMapper;
  /** Shared opaque-token codec; signing keys belong to the composition root. */
  private final CursorCodec cursorCodec;

  /**
   * Creates the PostgreSQL browse adapter.
   *
   * @param jdbc JDBC template for catalog queries
   * @param objectMapper mapper for JSON option data
   */
  public CatalogBrowseRepository(JdbcTemplate jdbc, ObjectMapper objectMapper, CursorCodec cursorCodec) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.cursorCodec = cursorCodec;
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
    ProductListingSql listing = productListingSql(path, categoryId, query);
    long total = jdbc.queryForObject(PRODUCT_ROWS_CTE + "SELECT COUNT(*) FROM product_rows" + listing.countFilters(),
        Long.class, listing.countParameters().toArray());
    List<Object> pageParameters = new ArrayList<>(listing.pageParameters());
    pageParameters.add(query.size() + 1);
    List<ProductRow> rows = jdbc.query(PRODUCT_ROWS_CTE + "SELECT * FROM product_rows" + listing.pageFilters()
            + " ORDER BY " + listing.orderBy() + " LIMIT ?", this::productRow, pageParameters.toArray());
    boolean hasMore = rows.size() > query.size();
    if (hasMore) {
      rows = rows.subList(0, query.size());
    }
    String next = hasMore ? encodeCursor(categoryId, query, rows.getLast()) : null;
    return new ProductPage(rows.stream().map(this::productView).toList(), next, total);
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

  /** Builds the parameterized database filter and stable ordering for a product listing. */
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
    String countFilters = filters.toString();
    List<Object> countParameters = List.copyOf(parameters);
    Cursor cursor = decodeCursor(categoryId, query);
    if (cursor != null) {
      appendSeekPredicate(filters, parameters, query.sort(), cursor);
    }
    return new ProductListingSql(countFilters, countParameters, filters.toString(), List.copyOf(parameters), orderBy(query.sort()));
  }

  /** Returns a fixed SQL order expression for each supported sort, including an ID tie-breaker. */
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

  /** Adds the keyset predicate immediately after the sort position encoded in the cursor. */
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
      case "createdAt:asc", "createdAt:desc" -> OffsetDateTime.ofInstant(cursor.sortValue().instantValue(), java.time.ZoneOffset.UTC);
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

  /** Encodes the typed sort position and product ID for a continuation request. */
  private String encodeCursor(UUID categoryId, ListingQuery query, ProductRow row) {
    return cursorCodec.encode(cursorContext(categoryId, query), List.of(sortValue(query.sort(), row)), row.id());
  }

  /** Decodes and validates a cursor before it is used to construct a seek predicate. */
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

  /** Binds a token to every input that selects or orders category-product results. */
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

  /** Validates the cursor's typed sort value before it reaches JDBC. */
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

  /** Parameterized SQL fragments shared by the full-count and seek-page queries. */
  private record ProductListingSql(String countFilters, List<Object> countParameters, String pageFilters,
      List<Object> pageParameters, String orderBy) {
  }

  /** Last row of a keyset cursor, represented by its active sort value and unique product ID. */
  private record Cursor(CursorValue sortValue, UUID productId) {
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
