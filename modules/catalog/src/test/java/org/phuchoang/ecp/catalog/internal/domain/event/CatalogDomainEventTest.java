package org.phuchoang.ecp.catalog.internal.domain.event;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogDomainEventTest {

    @Test
    void productFactsExposeTheCanonicalEventAndAggregateIdentity() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, UUID.randomUUID(), "Shirt", "shirt", null, null, PublicationStatus.DRAFT, null,
            Map.of(), List.of(new Product.Variant(UUID.randomUUID(), "SHIRT-M", "M", BigDecimal.TEN, "VND",
            Map.of(), null, true)), List.of());

        CatalogDomainEvent created = new ProductCreated(product);
        CatalogDomainEvent updated = new ProductUpdated(product);
        CatalogDomainEvent variantAdded = new VariantAdded(productId, product.categoryId(), product.variants().getFirst());

        assertThat(created.eventType()).isEqualTo("ProductCreated");
        assertThat(updated.eventType()).isEqualTo("ProductUpdated");
        assertThat(variantAdded.eventType()).isEqualTo("VariantAdded");
        assertThat(variantAdded.aggregateId()).isEqualTo(productId);
    }
}
