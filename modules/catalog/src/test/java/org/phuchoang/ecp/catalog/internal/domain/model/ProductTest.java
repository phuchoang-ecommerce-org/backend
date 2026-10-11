package org.phuchoang.ecp.catalog.internal.domain.model;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — the product aggregate owns immutable snapshots of its variants and images. */
class ProductTest {

    @Test
    void ownsImmutableSnapshotsOfItsChildrenAndAttributes() {
        Map<String, Object> attributes = new HashMap<>(Map.of("material", "cotton"));
        Map<String, String> options = new HashMap<>(Map.of("size", "M"));
        List<Product.Variant> variants = new ArrayList<>(List.of(new Product.Variant(
            UUID.randomUUID(), "SHIRT-M", "Medium", BigDecimal.TEN, "USD", options, 200, true)));
        List<Product.Image> images = new ArrayList<>(List.of(new Product.Image(
            UUID.randomUUID(), "https://example.test/shirt.jpg", "Shirt", 0)));

        Product product = new Product(UUID.randomUUID(), UUID.randomUUID(), "Shirt", "shirt", null, null,
            PublicationStatus.DRAFT, null, attributes, variants, images);
        attributes.put("color", "blue");
        options.put("color", "blue");
        variants.clear();
        images.clear();

        assertThat(product.attributes()).hasSize(1).containsEntry("material", "cotton");
        assertThat(product.variants()).singleElement().satisfies(variant ->
            assertThat(variant.options()).hasSize(1).containsEntry("size", "M"));
        assertThat(product.images()).hasSize(1);
        assertThatThrownBy(() -> product.variants().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void firstPublicationStampsPublishedAtAndLaterTransitionsKeepIt() {
        Instant first = Instant.parse("2026-09-18T10:00:00Z");
        Product draft = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Shirt", null, null, Map.of());
        assertThat(draft.publicationStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(draft.publishedAt()).isNull();

        draft.transitionTo(PublicationStatus.PUBLISHED, first);
        assertThat(draft.publishedAt()).isEqualTo(first);
        draft.transitionTo(PublicationStatus.UNPUBLISHED, first.plusSeconds(60));
        assertThat(draft.publicationStatus()).isEqualTo(PublicationStatus.UNPUBLISHED);
        draft.transitionTo(PublicationStatus.PUBLISHED, first.plusSeconds(120));
        assertThat(draft.publishedAt()).isEqualTo(first);
    }

    @Test
    void changingMerchandisingFieldsLeavesPublicationUntouched() {
        Product published = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Shirt", null, null, Map.of())
            .transitionTo(PublicationStatus.PUBLISHED, Instant.parse("2026-09-18T10:00:00Z"));

        Product changed = published.change(UUID.randomUUID(), "Tee", "Soft", "ECP", Map.of("fit", "slim"));

        assertThat(changed.publicationStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(changed.publishedAt()).isEqualTo(published.publishedAt());
        assertThat(changed.slug()).isEqualTo(published.slug());
        assertThat(changed.name()).isEqualTo("Tee");
    }

    @Test
    void slugsFollowTheSharedCatalogAlgorithmWithAnIdSuffix() {
        UUID id = UUID.randomUUID();
        Product product = Product.create(id, UUID.randomUUID(), "  Travel Mug: 500ml! ", null, null, Map.of());
        assertThat(product.slug()).isEqualTo("travel-mug-500ml-" + id.toString().substring(0, 8));
        assertThat(Slugs.normalize("Café & Bar")).isEqualTo("caf-bar");
    }

    @Test
    void ownsItsRepositoryPersistenceOperations() {
        ProductRepository repository = mock(ProductRepository.class);
        Product product = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Shirt", null, null, Map.of());
        when(repository.save(product)).thenReturn(product);

        product.create(repository);
        product.transitionTo(PublicationStatus.PUBLISHED, Instant.parse("2026-09-18T10:00:00Z"), repository);
        product.discontinue(repository);

        verify(repository, org.mockito.Mockito.times(2)).save(product);
        verify(repository).deleteById(product.id());
    }
}
