package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaProductRepositoryAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-17T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void persistsANewProductAggregateThroughTheDomainRepositoryPort() {
        CatalogProductJpaRepository products = mock(CatalogProductJpaRepository.class);
        UUID id = UUID.randomUUID();
        when(products.findAggregateById(id)).thenReturn(Optional.empty());
        when(products.save(any(CatalogProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        JpaProductRepositoryAdapter adapter = adapter(products);

        Product saved = adapter.save(Product.create(id, UUID.randomUUID(), "Travel Mug", "Insulated", "ECP",
            Map.of("capacityMl", 500)));

        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.slug()).startsWith("travel-mug-");
        assertThat(saved.attributes()).containsEntry("capacityMl", 500);
        verify(products).save(any(CatalogProductEntity.class));
    }

    @Test
    void persistsAggregateOwnedVariantsWithoutAChildRepository() {
        CatalogProductJpaRepository products = mock(CatalogProductJpaRepository.class);
        UUID id = UUID.randomUUID();
        when(products.findAggregateById(id)).thenReturn(Optional.empty());
        when(products.save(any(CatalogProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        JpaProductRepositoryAdapter adapter = adapter(products);
        Product product = Product.create(id, UUID.randomUUID(), "Travel Mug", null, null, Map.of())
            .addVariant(new Product.Variant(UUID.randomUUID(), "MUG-001", "Blue", new BigDecimal("12.50"), "USD",
                Map.of("colour", "blue"), null, true));

        Product saved = adapter.save(product);

        assertThat(saved.variants()).singleElement().extracting(Product.Variant::sku).isEqualTo("MUG-001");
        verify(products).save(any(CatalogProductEntity.class));
        verify(products).flush();
    }

    private static JpaProductRepositoryAdapter adapter(CatalogProductJpaRepository products) {
        ProductJpaMapper mapper = new ProductJpaMapper(new ObjectMapper());
        return new JpaProductRepositoryAdapter(products, mapper, new ProductChildSynchronizer(mapper, CLOCK), CLOCK);
    }
}
