package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter for the {@link Product} aggregate persistence port. */
@Repository
class JpaProductRepositoryAdapter implements ProductRepository {
    private final CatalogProductJpaRepository products;
    private final ProductJpaMapper mapper;
    private final ProductChildSynchronizer children;
    private final Clock clock;

    JpaProductRepositoryAdapter(CatalogProductJpaRepository products, ProductJpaMapper mapper,
            ProductChildSynchronizer children, Clock clock) {
        this.products = products;
        this.mapper = mapper;
        this.children = children;
        this.clock = clock;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return products.findAggregateById(id).map(mapper::toDomain);
    }

    @Override
    public Product save(Product product) {
        CatalogProductEntity existing = products.findAggregateById(product.id()).orElse(null);
        CatalogProductEntity entity;
        boolean addedVariant;
        if (existing == null) {
            entity = mapper.create(product, Instant.now(clock));
            addedVariant = !product.variants().isEmpty();
        } else {
            entity = existing;
            entity.update(product.name(), product.description(), product.brand(), product.categoryId(), mapper.json(product.attributes()),
                product.publicationStatus(), Instant.now(clock));
            addedVariant = children.synchronize(entity, product);
        }
        CatalogProductEntity saved = products.save(entity);
        // The administration service translates the database-wide SKU constraint into the
        // catalog validation error, so surface that constraint within save rather than at commit.
        if (addedVariant) products.flush();
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        products.deleteById(id);
    }

    @Override
    public long countByCategoryId(UUID categoryId) {
        return products.countByCategoryId(categoryId);
    }

}
