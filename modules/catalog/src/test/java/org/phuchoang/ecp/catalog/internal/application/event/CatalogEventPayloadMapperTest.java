package org.phuchoang.ecp.catalog.internal.application.event;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.event.payload.CategoryChangedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.CategoryRemovedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductChangedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductDiscontinuedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductPriceChangedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.VariantAddedPayload;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPriceChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPublished;
import org.phuchoang.ecp.catalog.internal.domain.event.VariantAdded;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;
import org.phuchoang.ecp.catalog.internal.domain.model.SubtreeCategory;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The v1 payload shapes, including the additive id fields the cache invalidator relies on. */
class CatalogEventPayloadMapperTest {

    private final CatalogEventPayloadMapper mapper = new CatalogEventPayloadMapper();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void productEventsCarryTheWholeProductAndEveryAffectedCategoryAsIdsAndSlugs() {
        Product product = product();
        UUID categoryId = product.categoryId();
        AffectedCategories affected = AffectedCategories.of(List.of(new SubtreeCategory(categoryId, "shirts"),
            new SubtreeCategory(UUID.randomUUID(), "tees")));

        Object payload = mapper.payload(new ProductPublished(product), affected);

        assertThat(payload).isInstanceOf(ProductChangedPayload.class);
        ProductChangedPayload changed = (ProductChangedPayload) payload;
        assertThat(changed.productId()).isEqualTo(product.id());
        assertThat(changed.variantIds()).containsExactly(product.variants().getFirst().id());
        assertThat(changed.variantSkus()).containsExactly("SHIRT-M");
        assertThat(changed.affectedCategoryIds()).hasSize(2).contains(categoryId);
        assertThat(changed.affectedCategorySlugs()).containsExactly("shirts", "tees");
        assertThat(changed.product().publicationStatus()).isEqualTo("PUBLISHED");
        assertThat(changed.product().variants().getFirst().listPrice().amount()).isEqualTo("10.50");

        @SuppressWarnings("unchecked")
        Map<String, Object> tree = json.readValue(json.writeValueAsString(payload), Map.class);
        assertThat(tree.keySet()).contains("productId", "variantIds", "variantSkus", "affectedCategoryIds",
            "affectedCategorySlugs", "product");
        @SuppressWarnings("unchecked")
        Map<String, Object> productTree = (Map<String, Object>) tree.get("product");
        assertThat(productTree.keySet()).contains("description"); // nulls stay explicit, as before
    }

    @Test
    void variantAndPriceEventsNameTheSingleVariant() {
        Product product = product();
        Product.Variant variant = product.variants().getFirst();

        VariantAddedPayload added = (VariantAddedPayload) mapper.payload(
            new VariantAdded(product.id(), product.categoryId(), variant),
            AffectedCategories.of(List.of(new SubtreeCategory(product.categoryId(), "shirts"))));
        ProductPriceChangedPayload priced = (ProductPriceChangedPayload) mapper.payload(
            new ProductPriceChanged(product.id(), variant), AffectedCategories.NONE);

        assertThat(added.variantId()).isEqualTo(variant.id());
        assertThat(added.variantIds()).containsExactly(variant.id());
        assertThat(added.affectedCategorySlugs()).containsExactly("shirts");
        assertThat(priced.variantId()).isEqualTo(variant.id());
        assertThat(priced.listPrice().currency()).isEqualTo("USD");
    }

    @Test
    void discontinuationListsIdsAndSkus() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        ProductDiscontinuedPayload payload = (ProductDiscontinuedPayload) mapper.payload(
            new ProductDiscontinued(productId, List.of(variantId), List.of("SHIRT-M")), AffectedCategories.NONE);

        assertThat(payload.variantIds()).containsExactly(variantId);
        assertThat(payload.variantSkus()).containsExactly("SHIRT-M");
    }

    @Test
    void categoryChangesDescribeTheCategoryWhileRemovalsKeepOnlyItsIdentity() {
        Category category = new Category(UUID.randomUUID(), null, "Shirts", "shirts", "/x/", 0, null, 3, false);
        AffectedCategories affected = AffectedCategories.of(List.of(new SubtreeCategory(category.id(), "shirts")));

        CategoryChangedPayload changed = (CategoryChangedPayload) mapper.payload(CategoryChanged.of(category), affected);
        CategoryRemovedPayload removed = (CategoryRemovedPayload) mapper.payload(CategoryChanged.removed(category),
            AffectedCategories.NONE);

        assertThat(changed.slug()).isEqualTo("shirts");
        assertThat(changed.sortOrder()).isEqualTo(3);
        assertThat(changed.affectedCategoryIds()).containsExactly(category.id());
        assertThat(removed.id()).isEqualTo(category.id());
        assertThat(removed.affectedCategorySlugs()).containsExactly("shirts");
        assertThat(removed.affectedCategoryIds()).containsExactly(category.id());
    }

    private static Product product() {
        return new Product(UUID.randomUUID(), UUID.randomUUID(), "Shirt", "shirt-1", null, "ECP",
            PublicationStatus.PUBLISHED, Instant.parse("2026-09-18T10:00:00Z"), Map.of(),
            List.of(new Product.Variant(UUID.randomUUID(), "SHIRT-M", "M", new BigDecimal("10.50"), "USD", Map.of(),
                null, true)), List.of());
    }
}
