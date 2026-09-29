package org.phuchoang.ecp.web.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.catalog.api.facade.CatalogAdministrationFacade;
import org.phuchoang.ecp.catalog.api.request.ProductWrite;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.configuration.security.NoAuthenticationSecurityConfig;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.common.security.NoAuthenticationRequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogAdministrationController.class)
@ActiveProfiles("no-auth")
@Import({NoAuthenticationSecurityConfig.class, NoAuthenticationRequestContextResolver.class})
class NoAuthenticationCatalogAdministrationControllerTest {

    private static final UUID LOCAL_ACCOUNT_ID =
        UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogAdministrationFacade catalog;

    @MockitoBean
    private RateLimiter rateLimiter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void allowEveryRequest() {
        BDDMockito.given(rateLimiter.tryConsume(anyString(), anyString()))
            .willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void createsAProductWithoutAuthenticationUsingTheAllAccessLocalActor() throws Exception {
        ProductDetailView created = new ProductDetailView(UUID.randomUUID(), "Mug", "mug", null, null, "DRAFT",
            null, List.of(), Map.of(), List.of(), List.of(), null, 0);
        BDDMockito.given(catalog.createProduct(any(), any(), any(ProductWrite.class))).willReturn(created);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mug\"}"))
            .andExpect(status().isCreated());

        ArgumentCaptor<IdentityActor> caller = ArgumentCaptor.forClass(IdentityActor.class);
        BDDMockito.then(catalog).should().createProduct(caller.capture(), any(), any(ProductWrite.class));
        assertThat(caller.getValue().accountId()).isEqualTo(LOCAL_ACCOUNT_ID);
        assertThat(caller.getValue().roles()).contains("GUEST", "CUSTOMER", "STAFF", "ADMINISTRATOR");
    }
}
