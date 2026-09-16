package org.phuchoang.ecp.web.ordering;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.OrderPageView;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.security.JwtKeysConfig;
import org.phuchoang.ecp.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L3 — `listOrders` (`UC-CUS-10`): always an empty page this sprint (see `ListOrdersService`). */
@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderFacade orderFacade;

    @MockitoBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        BDDMockito.given(rateLimiter.tryConsume(anyString(), anyString()))
            .willReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void listOrdersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrdersReturnsAnEmptyPageEnvelope() throws Exception {
        BDDMockito.given(orderFacade.listOrders(any(), any(), anyInt(), any())).willReturn(OrderPageView.EMPTY);

        mockMvc.perform(get("/api/v1/orders")
                .with(jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
                    .claim("roles", Set.of("CUSTOMER")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.page.next").doesNotExist());
    }

    @Test
    void listOrdersRejectsAnUnknownQueryParameter() throws Exception {
        mockMvc.perform(get("/api/v1/orders?bogus=1")
                .with(jwt().jwt(builder -> builder.subject("018f3c2a-0000-7000-8000-000000000000")
                    .claim("roles", Set.of("CUSTOMER")))))
            .andExpect(status().isBadRequest());
    }
}
