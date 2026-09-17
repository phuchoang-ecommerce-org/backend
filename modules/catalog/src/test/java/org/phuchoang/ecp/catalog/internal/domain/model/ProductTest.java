package org.phuchoang.ecp.catalog.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            "DRAFT", null, attributes, variants, images);
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
}
