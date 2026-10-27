package org.phuchoang.ecp.web.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phuchoang.ecp.catalog.api.browse.categoryproduct.CatalogListingQuery;
import org.phuchoang.ecp.catalog.api.facade.CatalogBrowseFacade;
import org.phuchoang.ecp.catalog.api.view.common.MoneyView;
import org.phuchoang.ecp.catalog.api.view.product.ProductPageView;
import org.phuchoang.ecp.catalog.api.view.product.ProductSummaryView;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.web.common.ratelimit.RateLimiter;
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

@WebMvcTest(controllers = CatalogBrowseController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class, JwtRequestContextResolver.class})
class CatalogBrowseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogBrowseFacade catalog;
    @MockitoBean private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        given(rateLimiter.tryConsume(anyString(), anyString())).willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void servesTheCompletePublishedCatalogToAGuest() throws Exception {
        ProductSummaryView product = new ProductSummaryView(UUID.randomUUID(), "Demo mug", "demo-mug", "ECP",
            "PUBLISHED", null, new MoneyView(new BigDecimal("24.0000"), "USD"),
            new MoneyView(new BigDecimal("24.0000"), "USD"), null, 0, true);
        given(catalog.listProducts(any())).willReturn(new ProductPageView(List.of(product), "next-page", 1));

        mockMvc.perform(get("/api/v1/products").param("size", "24"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Demo mug"))
            .andExpect(jsonPath("$.items[0].priceFrom.amount").value("24.0000"))
            .andExpect(jsonPath("$.page.next").value("next-page"))
            .andExpect(jsonPath("$.page.total").value(1));

        ArgumentCaptor<CatalogListingQuery> query = ArgumentCaptor.forClass(CatalogListingQuery.class);
        verify(catalog).listProducts(query.capture());
        assertThat(query.getValue().size()).isEqualTo(24);
        assertThat(query.getValue().sort()).isEqualTo("default");
    }
}
