package org.phuchoang.ecp.web.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.catalog.api.facade.CatalogProductFacade;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.RatingSummaryView;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.security.JwtKeysConfig;
import org.phuchoang.ecp.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class ProductControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogProductFacade catalog;
    @MockitoBean private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        BDDMockito.given(rateLimiter.tryConsume(anyString(), anyString()))
            .willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void servesGuestProductDetailAndOmitsUnknownAvailability() throws Exception {
        UUID id = UUID.randomUUID();
        BDDMockito.given(catalog.getProduct(id)).willReturn(new ProductDetailView(id, "Trail jacket", "trail-jacket",
            "Waterproof", "Northpeak", "PUBLISHED", null, List.of(), Map.of("waterproof", true), List.of(), List.of(), null, 0));

        mockMvc.perform(get("/api/v1/products/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Trail jacket"))
            .andExpect(jsonPath("$.variants").isArray())
            .andExpect(jsonPath("$.reviews").doesNotExist())
            .andExpect(jsonPath("$.relatedProducts").doesNotExist());
    }

    @Test
    void mapsAbsentAndUnpublishedProductsToTheSame404() throws Exception {
        UUID id = UUID.randomUUID();
        BDDMockito.given(catalog.getProduct(id)).willThrow(new DomainException(GenErrorCode.NOT_FOUND, "Product not found."));

        mockMvc.perform(get("/api/v1/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void returnsTheStableEmptyRatingSummary() throws Exception {
        UUID id = UUID.randomUUID();
        BDDMockito.given(catalog.getProductRatingSummary(id)).willReturn(RatingSummaryView.EMPTY);

        mockMvc.perform(get("/api/v1/products/{id}/rating-summary", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").doesNotExist())
            .andExpect(jsonPath("$.reviewCount").value(0))
            .andExpect(jsonPath("$.distribution.1").value(0))
            .andExpect(jsonPath("$.distribution.5").value(0));
    }

    @Test
    void noLongerMapsTheDemoRoute() throws Exception {
        mockMvc.perform(get("/api/v1/demo-products/{id}", UUID.randomUUID()).with(jwt()))
            .andExpect(status().isNotFound());
    }
}
