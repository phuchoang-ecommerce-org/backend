package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repository for the Category aggregate root only. */
interface CatalogCategoryJpaRepository extends JpaRepository<CatalogCategoryEntity, UUID> {
    long countByParentId(UUID parentId);
}
