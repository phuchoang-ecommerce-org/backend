package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.category;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** JDBC-only persistence support for category materialized-path operations. */
@Repository
class JdbcCategoryHierarchyStore {
    private final JdbcClient jdbc;
    private final EntityManager entityManager;

    JdbcCategoryHierarchyStore(JdbcClient jdbc, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    void relocateSubtree(String previousPath, int previousDepth, String updatedPath, int updatedDepth) {
        jdbc.sql("""
            UPDATE catalog_category SET path = :path || substring(path from :suffixStart),
                depth = depth + :depthDelta, updated_at = now(), version = version + 1
            WHERE path LIKE :previousPath
            """)
            .param("path", updatedPath)
            .param("suffixStart", previousPath.length() + 1)
            .param("depthDelta", updatedDepth - previousDepth)
            .param("previousPath", previousPath + "%")
            .update();
        entityManager.clear();
    }

    List<String> findSlugsInSubtree(UUID categoryId) {
        return jdbc.sql("""
            SELECT slug FROM catalog_category
            WHERE path IN (SELECT path FROM catalog_category WHERE id = :id)
               OR path LIKE (SELECT path || '%' FROM catalog_category WHERE id = :id)
            ORDER BY depth
            """).param("id", categoryId).query(String.class).list();
    }
}
