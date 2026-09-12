package org.phuchoang.ecp.web.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.identity.api.AccountView;
import org.phuchoang.ecp.identity.api.Actor;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.identity.api.LoginRequest;
import org.phuchoang.ecp.identity.api.SessionResponse;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.RateLimiter;
import org.phuchoang.ecp.security.JwtKeysConfig;
import org.phuchoang.ecp.security.SecurityConfig;
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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L3 — `logIn`/`logOut` (`UC-CUS-03`, `UC-CUS-04`). Gate G1 check 5: an unknown-account failure
 * and a wrong-password failure must render byte-identically — enforced at L1 in
 * {@code LoginServiceTest}; this only confirms the controller doesn't add anything on top.
 */
@WebMvcTest(controllers = SessionController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class SessionControllerTest {

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

    @Test
    void successfulLoginReturns201WithASessionBody() throws Exception {
        AccountView account = new AccountView("018f3c2a-0000-7000-8000-000000000000", "customer@example.com",
            "ACTIVE", "VERIFIED", Set.of("CUSTOMER"), Instant.parse("2026-09-01T00:00:00Z"), null,
            Instant.parse("2026-09-01T00:00:00Z"));
        SessionResponse session = new SessionResponse("access-token", "refresh-token", 900, false, account);
        BDDMockito.given(identityFacade.logIn(any(LoginRequest.class))).willReturn(session);

        mockMvc.perform(post("/api/v1/sessions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"customer@example.com\",\"password\":\"Str0ngPassword\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.restricted").value(false))
            .andExpect(jsonPath("$.account.email").value("customer@example.com"));
    }

    @Test
    void unknownAccountAndWrongPasswordRenderTheSameProblemBody_BR_CUS_04() throws Exception {
        DomainException genericFailure = new DomainException(GenErrorCode.NOT_AUTHENTICATED,
            "Email or password is incorrect.");
        BDDMockito.given(identityFacade.logIn(any(LoginRequest.class))).willThrow(genericFailure);

        String fixedCorrelationId = "0f9c2b3a-4d61-4e2f-9c77-1a2b3c4d5e6f";
        String unknownAccountBody = mockMvc.perform(post("/api/v1/sessions").with(csrf())
                .header("X-Correlation-Id", fixedCorrelationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"unknown@example.com\",\"password\":\"whatever1\"}"))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        String wrongPasswordBody = mockMvc.perform(post("/api/v1/sessions").with(csrf())
                .header("X-Correlation-Id", fixedCorrelationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"customer@example.com\",\"password\":\"wrongPassword1\"}"))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(unknownAccountBody).isEqualTo(wrongPasswordBody);
    }

    @Test
    void logOutRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/sessions/current").with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedLogOutInvalidatesTheGivenRefreshTokenAndReturns204() throws Exception {
        String accountId = "018f3c2a-0000-7000-8000-000000000000";

        mockMvc.perform(delete("/api/v1/sessions/current").with(csrf())
                .with(jwt().jwt(builder -> builder.subject(accountId).claim("roles", Set.of("CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"some-refresh-token\"}"))
            .andExpect(status().isNoContent());

        verify(identityFacade).logOut(
            org.mockito.ArgumentMatchers.eq(new Actor(java.util.UUID.fromString(accountId), Set.of("CUSTOMER"))),
            org.mockito.ArgumentMatchers.eq("some-refresh-token"));
    }
}
