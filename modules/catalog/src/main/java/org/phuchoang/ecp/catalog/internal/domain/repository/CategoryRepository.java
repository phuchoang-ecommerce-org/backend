package org.phuchoang.ecp.catalog.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.SubtreeCategory;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/** Repository boundary for the {@link Category} aggregate root. */
@Repository
public interface CategoryRepository {

    Optional<Category> findById(UUID id);

    Category save(Category category);

    void deleteById(UUID id);

    long countByParentId(UUID parentId);

    /** The category itself and every descendant, shallowest first — the listings a change affects. */
    List<SubtreeCategory> findSubtree(UUID categoryId);
}
