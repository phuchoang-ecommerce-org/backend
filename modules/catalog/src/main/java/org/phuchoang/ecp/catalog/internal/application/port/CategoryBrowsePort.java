package org.phuchoang.ecp.catalog.internal.application.port;

import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryDetail;
import org.phuchoang.ecp.catalog.internal.application.query.model.category.CategoryNode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read port for category navigation; implementations return projections, never aggregates. */
public interface CategoryBrowsePort {

    /** Whether a category row exists, regardless of whether it has products. */
    boolean categoryExists(UUID id);

    /** One category and its breadcrumb chain, or empty when absent. */
    Optional<CategoryDetail> category(UUID id);

    /** The complete category tree, recursively nested from the roots. */
    List<CategoryNode> wholeTree();

    /** A complete tree or a depth-bounded subtree; {@code null} root loads all roots. */
    List<CategoryNode> tree(UUID rootId, Integer maxDepth);
}
