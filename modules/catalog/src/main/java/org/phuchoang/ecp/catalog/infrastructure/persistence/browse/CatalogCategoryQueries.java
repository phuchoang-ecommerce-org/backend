package org.phuchoang.ecp.catalog.infrastructure.persistence.browse;

import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.Category;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.CategoryNode;
import org.phuchoang.ecp.catalog.application.query.CatalogBrowseModel.CategoryRef;
import org.phuchoang.ecp.catalog.infrastructure.persistence.JdbcQuerySupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Catalog-owned category existence, hierarchy, and breadcrumb queries. */
@Component
class CatalogCategoryQueries extends JdbcQuerySupport {

  CatalogCategoryQueries(JdbcTemplate jdbc) {
    super(jdbc);
  }

  boolean exists(UUID id) {
    return exists("SELECT EXISTS (SELECT 1 FROM catalog_category WHERE id = ?)", id);
  }

  Optional<Category> category(UUID id) {
    Optional<CategoryRow> category = optional("""
        SELECT id, parent_id, name, slug, path, depth, sort_order
        FROM catalog_category WHERE id = ?
        """, this::categoryRow, id);
    if (category.isEmpty()) {
      return Optional.empty();
    }
    CategoryRow row = category.get();
    return Optional.of(new Category(row.id, row.parentId, row.name, row.slug, row.depth, row.sortOrder, null, false,
        ancestors(row)));
  }

  List<CategoryNode> wholeTree() {
    return tree(null, null);
  }

  List<CategoryNode> tree(UUID rootId, Integer maxDepth) {
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

  private CategoryRow categoryRow(ResultSet rs, int ignored) throws SQLException {
    return new CategoryRow(rs.getObject("id", UUID.class), rs.getObject("parent_id", UUID.class), rs.getString("name"),
        rs.getString("slug"), rs.getString("path"), rs.getInt("depth"), rs.getInt("sort_order"));
  }

  private List<CategoryRef> ancestors(CategoryRow row) {
    if (row.parentId == null) {
      return List.of();
    }
    return jdbc.query("""
        SELECT id, name, slug FROM catalog_category
        WHERE ? LIKE path || '%' AND id <> ? ORDER BY depth
        """, (rs, ignored) -> new CategoryRef(rs.getObject("id", UUID.class), rs.getString("name"),
        rs.getString("slug")), row.path, row.id);
  }

  private record CategoryRow(UUID id, UUID parentId, String name, String slug, String path, int depth, int sortOrder) {
  }

  private static final class MutableNode {
    private final CategoryRow row;
    private final List<CategoryRef> ancestors;
    private final List<MutableNode> children = new ArrayList<>();

    private MutableNode(CategoryRow row, List<CategoryRef> ancestors) {
      this.row = row;
      this.ancestors = ancestors;
    }

    private CategoryNode freeze() {
      return new CategoryNode(row.id, row.parentId, row.name, row.slug, row.depth, row.sortOrder, null, false,
          ancestors, children.stream().map(MutableNode::freeze).toList());
    }
  }
}
