package org.phuchoang.ecp.catalog.infrastructure.persistence.write;

import org.phuchoang.ecp.catalog.application.command.CreateCategoryCommand;
import org.phuchoang.ecp.catalog.application.port.CatalogCategoryWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** PostgreSQL adapter for the catalog aggregate's minimal internal creation command. */
@Repository
public class JdbcCatalogCategoryWriter implements CatalogCategoryWriter {

    private final JdbcTemplate jdbc;

    public JdbcCatalogCategoryWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public CreatedCategory create(CreateCategoryCommand command) {
        Parent parent = parent(command);
        int depth = parent == null ? 0 : parent.depth + 1;
        String path = parent == null ? "/" + command.id() + "/" : parent.path + command.id() + "/";
        jdbc.update("""
            INSERT INTO catalog_category (id, parent_id, name, slug, path, depth, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, command.id(), command.parentId(), command.name(), command.slug(), path, depth, command.sortOrder());
        return new CreatedCategory(path, depth);
    }

    private Parent parent(CreateCategoryCommand command) {
        if (command.parentId() == null) {
            return null;
        }
        List<Parent> parents = jdbc.query("SELECT path, depth FROM catalog_category WHERE id = ?", (rs, row) ->
            new Parent(rs.getString("path"), rs.getInt("depth")), command.parentId());
        if (parents.isEmpty()) {
            throw new IllegalArgumentException("The category parent does not exist.");
        }
        return parents.getFirst();
    }

    private record Parent(String path, int depth) {
    }
}
