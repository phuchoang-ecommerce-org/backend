package org.phuchoang.ecp.web.ordering;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.ordering.api.CheckoutView;
import org.phuchoang.ecp.ordering.api.CheckoutFacade;
import org.phuchoang.ecp.ordering.api.CheckoutOutcome;
import org.phuchoang.ecp.web.common.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.common.security.JwtRequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 regression for the contract-sync entry point: create and resume use distinct HTTP statuses. */
@WebMvcTest(controllers = CheckoutController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class, JwtRequestContextResolver.class})
class CheckoutControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CheckoutFacade checkouts;
    @MockitoBean private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        when(rateLimiter.tryConsume(anyString(), anyString())).thenReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void initiateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/checkouts")).andExpect(status().isUnauthorized());
    }

    @Test
    void initiateCreatesCheckoutAtItsLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(checkouts.initiateCheckout(any())).thenReturn(new CheckoutOutcome(checkout(id), false));

        mockMvc.perform(post("/api/v1/checkouts").with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())
                .claim("roles", Set.of("CUSTOMER")))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/checkouts/" + id))
            .andExpect(jsonPath("$.order.id").value(id.toString()))
            .andExpect(jsonPath("$.ready").value(false));
    }

    private static CheckoutView checkout(UUID id) {
        CheckoutView.MoneyView zero = new CheckoutView.MoneyView(BigDecimal.ZERO, "USD");
        return new CheckoutView(new CheckoutView.OrderView(id, "DRAFT-1", UUID.randomUUID(), List.of(), zero, zero, zero,
            zero, null, null, null, null), List.of(), null, false, List.of());
    }
}
