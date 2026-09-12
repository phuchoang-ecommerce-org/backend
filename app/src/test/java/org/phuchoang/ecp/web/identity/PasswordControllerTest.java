package org.phuchoang.ecp.web.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.sharedkernel.api.RateLimiter;
import org.phuchoang.ecp.security.JwtKeysConfig;
import org.phuchoang.ecp.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 — `changeOwnPassword` (`UC-CUS-06`), `requestPasswordReset`/`completePasswordReset` (`UC-CUS-07`). */
@WebMvcTest(controllers = PasswordController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class PasswordControllerTest {

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
    void changeOwnPasswordRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/accounts/me/password").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"current\",\"newPassword\":\"new-Password1\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void changeOwnPasswordReturns204() throws Exception {
        mockMvc.perform(put("/api/v1/accounts/me/password").with(csrf())
                .with(jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
                    .claim("roles", Set.of("CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"current\",\"newPassword\":\"new-Password1\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void changeOwnPasswordRejectsAMissingNewPassword() throws Exception {
        mockMvc.perform(put("/api/v1/accounts/me/password").with(csrf())
                .with(jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
                    .claim("roles", Set.of("CUSTOMER"))))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"current\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
    }

    @Test
    void requestPasswordResetIsUnauthenticatedAndReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/password-reset-requests").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"customer@example.com\"}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void completePasswordResetIsUnauthenticatedAndReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/password-resets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"raw-token\",\"newPassword\":\"new-Password1\"}"))
            .andExpect(status().isNoContent());
    }
}
