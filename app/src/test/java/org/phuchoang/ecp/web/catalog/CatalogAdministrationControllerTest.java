package org.phuchoang.ecp.web.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.catalog.api.facade.CatalogAdministrationFacade;
import org.phuchoang.ecp.catalog.api.request.ProductWrite;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.common.security.JwtRequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 — the admin adapter resolves caller and correlation id from the request, not from a mandatory header. */
@WebMvcTest(controllers = CatalogAdministrationController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class, JwtRequestContextResolver.class})
class CatalogAdministrationControllerTest {

    private static final String STAFF_ID = "018f3c2a-0000-7000-8000-000000000001";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CatalogAdministrationFacade catalog;
    @MockitoBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        BDDMockito.given(rateLimiter.tryConsume(anyString(), anyString())).willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void createsAProductWithoutRequiringACorrelationHeaderAndPassesTheJwtCaller() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDetailView created = new ProductDetailView(productId, "Mug", "mug", null, null, "DRAFT", null,
            List.of(), Map.of(), List.of(), List.of(), null, 0);
        BDDMockito.given(catalog.createProduct(any(), any(), any(ProductWrite.class))).willReturn(created);

        mockMvc.perform(post("/api/v1/products")
                .with(jwt().jwt(builder -> builder.subject(STAFF_ID).claim("roles", Set.of("STAFF"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mug\",\"categoryId\":\"" + UUID.randomUUID() + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/products/" + productId))
            .andExpect(jsonPath("$.id").value(productId.toString()));

        ArgumentCaptor<IdentityActor> caller = ArgumentCaptor.forClass(IdentityActor.class);
        ArgumentCaptor<UUID> correlationId = ArgumentCaptor.forClass(UUID.class);
        BDDMockito.then(catalog).should().createProduct(caller.capture(), correlationId.capture(), any(ProductWrite.class));
        assertThat(caller.getValue()).isEqualTo(new IdentityActor(UUID.fromString(STAFF_ID), Set.of("STAFF")));
        assertThat(correlationId.getValue()).isNotNull();
    }

    @Test
    void anExplicitCorrelationHeaderIsPropagatedToTheCommand() throws Exception {
        UUID correlation = UUID.randomUUID();
        BDDMockito.given(catalog.createProduct(any(), any(), any(ProductWrite.class))).willReturn(
            new ProductDetailView(UUID.randomUUID(), "Mug", "mug", null, null, "DRAFT", null, List.of(), Map.of(),
                List.of(), List.of(), null, 0));

        mockMvc.perform(post("/api/v1/products")
                .with(jwt().jwt(builder -> builder.subject(STAFF_ID).claim("roles", Set.of("STAFF"))))
                .header("X-Correlation-Id", correlation.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mug\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("X-Correlation-Id", correlation.toString()));

        BDDMockito.then(catalog).should().createProduct(any(), org.mockito.ArgumentMatchers.eq(correlation), any());
    }

    @Test
    void administrationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
