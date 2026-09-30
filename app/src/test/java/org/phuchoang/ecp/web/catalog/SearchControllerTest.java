package org.phuchoang.ecp.web.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phuchoang.ecp.catalog.api.search.CatalogSearchFacade;
import org.phuchoang.ecp.catalog.api.search.SearchProductsQuery;
import org.phuchoang.ecp.catalog.api.search.view.ActiveSearchFilterView;
import org.phuchoang.ecp.catalog.api.search.view.SearchFacetView;
import org.phuchoang.ecp.catalog.api.search.view.SearchResultPageView;
import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.web.common.security.JwtRequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class, JwtRequestContextResolver.class, SearchHttpRequestMapper.class})
class SearchControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogSearchFacade search;
    @MockitoBean private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        given(rateLimiter.tryConsume(anyString(), anyString())).willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void returnsAnEmptySuccessfulResultWithAppliedFiltersAndFacets() throws Exception {
        given(search.searchProducts(any())).willReturn(new SearchResultPageView(List.of(), null, 0L,
            List.of(new SearchFacetView("brand", List.of(new SearchFacetView.Value("ECP", 0)))),
            List.of(new ActiveSearchFilterView("priceFrom", "10")), false));

        mockMvc.perform(get("/api/v1/search/products")
                .param("brand", "ECP")
                .param("priceFrom", "20")
                .param("priceTo", "10")
                .param("attribute", "colour:blue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.page.size").value(0))
            .andExpect(jsonPath("$.page.total").doesNotExist())
            .andExpect(jsonPath("$.matchCount").value(0))
            .andExpect(jsonPath("$.facets[0].field").value("brand"))
            .andExpect(jsonPath("$.activeFilters[0].field").value("priceFrom"));

        ArgumentCaptor<SearchProductsQuery> query = ArgumentCaptor.forClass(SearchProductsQuery.class);
        verify(search).searchProducts(query.capture());
        assertThat(query.getValue().priceFrom()).isEqualByComparingTo("10");
        assertThat(query.getValue().priceTo()).isEqualByComparingTo("20");
        assertThat(query.getValue().attributes()).containsEntry("colour", "blue");
        assertThat(query.getValue().sort()).isEqualTo("createdAt:desc");
    }

    @Test
    void keywordSearchUsesRelevanceAndReturnsCatalogProductShape() throws Exception {
        ProductSummaryView product = new ProductSummaryView(UUID.randomUUID(), "Trail jacket", "trail-jacket", "ECP",
            "PUBLISHED", null, null, null, null, 0, true);
        given(search.searchProducts(any())).willReturn(new SearchResultPageView(List.of(product), "opaque", 1L,
            List.of(), List.of(), false));

        mockMvc.perform(get("/api/v1/search/products").param("q", " jacket "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Trail jacket"))
            .andExpect(jsonPath("$.page.next").value("opaque"));

        ArgumentCaptor<SearchProductsQuery> query = ArgumentCaptor.forClass(SearchProductsQuery.class);
        verify(search).searchProducts(query.capture());
        assertThat(query.getValue().keyword()).isEqualTo("jacket");
        assertThat(query.getValue().sort()).isEqualTo("relevance");
    }

    @Test
    void rejectsTheDocumentedIncompatibleKeywordAndSortCombination() throws Exception {
        mockMvc.perform(get("/api/v1/search/products").param("q", "jacket").param("sort", "price:asc"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("ECP-GEN-4000"));
    }

    @Test
    void mapsSearchStoreFailureToTheCorrectGenericDependencyCode() throws Exception {
        given(search.searchProducts(any())).willThrow(new DomainException(GenErrorCode.DEPENDENCY_UNAVAILABLE,
            "Search is temporarily unavailable."));

        mockMvc.perform(get("/api/v1/search/products").param("q", "jacket"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("ECP-GEN-5030"));
    }
}
