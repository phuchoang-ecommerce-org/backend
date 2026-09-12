package org.phuchoang.ecp.web.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.identity.api.IdentityFacade;
import org.phuchoang.ecp.identity.api.RegisterAccountRequest;
import org.phuchoang.ecp.sharedkernel.api.DomainException;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 — `registerAccount` (`UC-CUS-01`), against `paths/identity.yaml#/accounts`'s `post`. */
@WebMvcTest(controllers = AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityFacade identityFacade;

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
    void validRegistrationReturns202Accepted() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"guest@example.com\",\"password\":\"Str0ngPassword\"}"))
            .andExpect(status().isAccepted());
    }

    @Test
    void missingEmailIsRejectedBeforeReachingTheFacade() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Str0ngPassword\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ECP-GEN-4000"))
            .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void aWeakPasswordFromTheDomainMapsToValidationFailed() throws Exception {
        doThrow(new DomainException(GenErrorCode.VALIDATION_FAILED, "Password must be at least 10 characters long."))
            .when(identityFacade).registerAccount(any(RegisterAccountRequest.class));

        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"guest@example.com\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ECP-GEN-4000"));
    }

    @Test
    void duplicateEmailStillReturns202_BR_CUS_04() throws Exception {
        // BR-CUS-04 — the application layer never surfaces a distinguishable outcome for this
        // case; registerAccount simply returns normally either way (see RegisterAccountService).
        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"already-registered@example.com\",\"password\":\"Str0ngPassword\"}"))
            .andExpect(status().isAccepted());
        verify(identityFacade).registerAccount(any(RegisterAccountRequest.class));
    }
}
