package org.phuchoang.ecp.web.shipping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.phuchoang.ecp.configuration.security.JwtKeysConfig;
import org.phuchoang.ecp.configuration.security.SecurityConfig;
import org.phuchoang.ecp.ordering.api.CheckoutView;
import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.OrderView;
import org.phuchoang.ecp.shipping.api.ShipmentView;
import org.phuchoang.ecp.shipping.api.ShippingFacade;
import org.phuchoang.ecp.web.common.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.common.security.JwtRequestContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShippingController.class)
@Import({SecurityConfig.class, JwtKeysConfig.class, JwtRequestContextResolver.class})
class ShippingControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ShippingFacade shipments;
    @MockitoBean private OrderFacade orders;
    @MockitoBean private RateLimiter rateLimiter;

    @BeforeEach
    void allowEveryRequest() {
        when(rateLimiter.tryConsume(anyString(), anyString())).thenReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void trackingRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}/tracking", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void trackingReturnsCurrentOrderAndCarrierReceiptTime() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        when(orders.getOrder(any(), BDDMockito.eq(orderId))).thenReturn(order(orderId));
        when(shipments.listShipments(any(), BDDMockito.eq(orderId))).thenReturn(List.of(new ShipmentView(
            shipmentId, orderId, "ACME", "TRK-1", "IN_TRANSIT", Instant.parse("2026-10-16T08:00:00Z"), null,
            Instant.parse("2026-10-16T08:00:00Z"), Instant.parse("2026-10-16T09:05:00Z"), List.of(
                new ShipmentView.TrackingEventView(eventId, "carrier-1", "IN_TRANSIT", "Collected", "Ho Chi Minh City",
                    Instant.parse("2026-10-16T09:00:00Z"), Instant.parse("2026-10-16T09:05:00Z"), true)))));

        mockMvc.perform(get("/api/v1/orders/{orderId}/tracking", orderId)
                .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()).claim("roles", Set.of("CUSTOMER")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId.toString()))
            .andExpect(jsonPath("$.shipments[0].createdAt").value("2026-10-16T08:00:00Z"))
            .andExpect(jsonPath("$.shipments[0].lastUpdateReceivedAt").value("2026-10-16T09:05:00Z"))
            .andExpect(jsonPath("$.shipments[0].trackingEvents[0].receivedAt").value("2026-10-16T09:05:00Z"));
    }

    @Test
    void warehouseOperatorCanCreateAnAutomaticCarrierShipment() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        when(orders.getOrder(any(), BDDMockito.eq(orderId))).thenReturn(order(orderId));
        when(shipments.createShipment(any(), BDDMockito.eq(orderId), BDDMockito.isNull(), BDDMockito.eq("70000"),
            BDDMockito.eq("VN"), any())).thenReturn(new ShipmentView(shipmentId, orderId, "DEVELOPMENT", "DEV-1",
                "DISPATCHED", Instant.parse("2026-10-16T08:00:00Z"), null, Instant.parse("2026-10-16T08:00:00Z"),
                null, List.of()));

        mockMvc.perform(post("/api/v1/shipments").contentType("application/json")
                .content("{\"orderId\":\"" + orderId + "\"}")
                .with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString())
                    .claim("roles", Set.of("WAREHOUSE_OPERATOR")))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.trackingReference").value("DEV-1"));
    }

    private static OrderView order(UUID id) {
        CheckoutView.MoneyView zero = new CheckoutView.MoneyView("0", "USD");
        CheckoutView.AddressView address = new CheckoutView.AddressView(null, "Mai", "1 Main Street", null,
            "Ho Chi Minh City", null, "70000", "VN", null);
        return new OrderView(id, "EC-1042", UUID.randomUUID(), "SHIPPING", "USD", List.of(), zero, zero, zero,
            zero, zero, null, null, address, address, Instant.parse("2026-10-16T08:00:00Z"),
            Instant.parse("2026-10-16T08:00:00Z"));
    }
}
