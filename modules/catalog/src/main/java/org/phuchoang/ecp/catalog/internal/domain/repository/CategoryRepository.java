package org.phuchoang.ecp.catalog.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;

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

    List<String> findSlugsInSubtree(UUID categoryId);
}
