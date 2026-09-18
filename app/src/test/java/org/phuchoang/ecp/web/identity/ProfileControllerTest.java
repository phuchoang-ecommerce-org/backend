package org.phuchoang.ecp.web.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.identity.api.facade.IdentityFacade;
import org.phuchoang.ecp.identity.api.request.ProfileUpdateRequest;
import org.phuchoang.ecp.identity.api.view.AccountView;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.web.common.security.JwtRequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 — `getOwnAccount`/`updateOwnProfile` (`UC-CUS-08`), against `paths/identity.yaml#/accountsMe`. */
@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class, JwtRequestContextResolver.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityFacade identityFacade;

    @MockitoBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        BDDMockito.given(rateLimiter.tryConsume(anyString(), anyString()))
            .willReturn(new RateLimiter.Decision(true, 0));
    }

    private static AccountView anAccountView() {
        return new AccountView("018f3c2a-0000-7000-8000-000000000000", "customer@example.com", "Customer",
            "ACTIVE", "VERIFIED", null, Set.of("CUSTOMER"), Instant.parse("2026-09-01T00:00:00Z"), null,
            Instant.parse("2026-09-01T00:00:00Z"));
    }

    @Test
    void getOwnAccountRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getOwnAccountReturnsTheCallersAccount() throws Exception {
        BDDMockito.given(identityFacade.getOwnAccount(any())).willReturn(anAccountView());

        mockMvc.perform(get("/api/v1/accounts/me")
                .with(jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
                    .claim("roles", Set.of("CUSTOMER")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("customer@example.com"));
    }

    @Test
    void updateOwnProfileReturnsTheUpdatedAccount() throws Exception {
        BDDMockito.given(identityFacade.updateOwnProfile(any(), any(ProfileUpdateRequest.class)))
            .willReturn(anAccountView());

        mockMvc.perform(patch("/api/v1/accounts/me").with(csrf())
                .with(jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
                    .claim("roles", Set.of("CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"New Name\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("customer@example.com"));
    }
}
