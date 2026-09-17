package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repository for the Product aggregate root only. */
interface CatalogProductJpaRepository extends JpaRepository<CatalogProductEntity, UUID> {
    @EntityGraph(attributePaths = { "variants", "images" })
    Optional<CatalogProductEntity> findAggregateById(UUID id);

    long countByCategoryId(UUID categoryId);
}
