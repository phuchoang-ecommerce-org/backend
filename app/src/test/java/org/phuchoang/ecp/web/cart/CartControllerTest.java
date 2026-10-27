package org.phuchoang.ecp.web.cart;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.api.CartFacade;
import org.phuchoang.ecp.cart.api.CartView;
import org.phuchoang.ecp.cart.api.MoneyView;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.web.common.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CartFacade carts;
    @MockitoBean private RequestContextResolver requestContext;
    @MockitoBean private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        given(rateLimiter.tryConsume(anyString(), anyString())).willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void serializesGuestCartMoneyAsAContractDecimalString() throws Exception {
        Instant now = Instant.parse("2026-10-27T00:00:00Z");
        given(requestContext.resolve(null)).willReturn(new RequestContext(UUID.randomUUID(), IdentityActor.GUEST));
        given(carts.getCurrentCart(IdentityActor.GUEST, "guest-token")).willReturn(new CartView(UUID.randomUUID(),
            null, "ACTIVE", List.of(), new MoneyView(new BigDecimal("0.0000"), "USD"), now,
            now.plusSeconds(604800), false));

        mockMvc.perform(get("/api/v1/carts/current").cookie(new Cookie("ecp_guest_cart", "guest-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotal.amount").value("0.0000"))
            .andExpect(jsonPath("$.subtotal.currency").value("USD"));
    }
}
