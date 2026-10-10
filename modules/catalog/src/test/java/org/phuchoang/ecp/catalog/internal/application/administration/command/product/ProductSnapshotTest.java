package org.phuchoang.ecp.catalog.internal.application.administration.command.product;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSnapshotTest {

    private final ProductSnapshotMapper mapper = Mappers.getMapper(ProductSnapshotMapper.class);

    @Test
    void preservesTheAdministrativeProductResultShape() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-10-11T10:15:30Z");
        Product product = new Product(productId, UUID.randomUUID(), "T-Shirt", "t-shirt", "Soft cotton", "ECP",
            PublicationStatus.PUBLISHED, publishedAt, Map.of("material", "cotton"), List.of(new Product.Variant(
                variantId, "TSHIRT-M", "Medium", new BigDecimal("19.99"), "USD", Map.of("size", "M"), 180, true)),
            List.of(new Product.Image(imageId, "https://example.test/t-shirt.jpg", "T-Shirt", 1)));

        ProductSnapshot snapshot = mapper.productSnapshot(product);

        assertThat(snapshot).extracting(ProductSnapshot::id, ProductSnapshot::publicationStatus,
            ProductSnapshot::publishedAt, ProductSnapshot::attributes)
            .containsExactly(productId, "PUBLISHED", OffsetDateTime.parse("2026-10-11T10:15:30Z"),
                Map.of("material", "cotton"));
        assertThat(snapshot.variants()).singleElement().extracting(variant -> variant.id(), variant -> variant.active())
            .containsExactly(variantId, true);
        assertThat(snapshot.images()).singleElement().extracting(image -> image.id(), image -> image.sortOrder())
            .containsExactly(imageId, 1);
    }
}
