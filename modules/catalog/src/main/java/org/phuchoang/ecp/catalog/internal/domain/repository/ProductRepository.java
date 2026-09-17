package org.phuchoang.ecp.catalog.internal.domain.repository;

import org.jmolecules.ddd.annotation.Repository;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.util.Optional;
import java.util.UUID;

/** Repository boundary for the {@link Product} aggregate root. */
@Repository
public interface ProductRepository {

    Optional<Product> findById(UUID id);

    Product save(Product product);

    void deleteById(UUID id);

    long countByCategoryId(UUID categoryId);
}
