package org.phuchoang.ecp.catalog.api.search;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.search.query.ActiveSearchFilter;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchFacet;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProduct;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductPage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSearchMapperTest {

    private final CatalogSearchMapper mapper = Mappers.getMapper(CatalogSearchMapper.class);

    @Test
    void preservesSearchQueryAndResponseContracts() {
        UUID productId = UUID.randomUUID();
        SearchProductsQuery query = new SearchProductsQuery(" shirt ", "cursor", 24, "PRICE_ASC", UUID.randomUUID(),
            List.of("ECP"), Map.of("size", "M"), BigDecimal.TEN, new BigDecimal("50.00"), true);
        SearchProductPage page = new SearchProductPage(List.of(new SearchProduct(productId, "T-Shirt", "t-shirt", "ECP",
            "https://example.test/t-shirt.jpg", new MoneyValue(BigDecimal.TEN, "USD"), null, 4.5, 12, true)), "next", 12L,
            List.of(new SearchFacet("brand", List.of(new SearchFacet.Value("ECP", 12)))),
            List.of(new ActiveSearchFilter("brand", "ECP")));

        assertThat(mapper.query(query)).isEqualTo(new org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductsQuery(
            "shirt", "cursor", 24, "PRICE_ASC", query.categoryId(), List.of("ECP"), Map.of("size", "M"),
            BigDecimal.TEN, new BigDecimal("50.00"), true));
        assertThat(mapper.page(page)).satisfies(view -> {
            assertThat(view.items()).singleElement().extracting(item -> item.id(), item -> item.publicationStatus(),
                item -> item.priceFrom()).containsExactly(productId, "PUBLISHED", new org.phuchoang.ecp.catalog.api.view.common.MoneyView(BigDecimal.TEN, "USD"));
            assertThat(view.facets()).singleElement().extracting(facet -> facet.field(), facet -> facet.values().getFirst().count())
                .containsExactly("brand", 12L);
            assertThat(view.activeFilters()).singleElement().extracting(filter -> filter.field(), filter -> filter.value())
                .containsExactly("brand", "ECP");
            assertThat(view.degraded()).isFalse();
        });
    }
}
