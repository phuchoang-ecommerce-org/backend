package org.phuchoang.ecp.web.shipping;

import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.OrderView;
import org.phuchoang.ecp.shipping.api.ShipmentView;
import org.phuchoang.ecp.shipping.api.ShippingFacade;
import org.phuchoang.ecp.web.common.pagination.Page;
import org.phuchoang.ecp.web.common.pagination.PageEnvelope;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;
import java.net.URI;

/**
 * Read-only HTTP adapter for customer shipment tracking.
 *
 * <p>Order lookup happens before each shipment read. {@link OrderFacade} turns
 * another customer's order into a not-found result, so this adapter never
 * reveals the existence of a shipment through a direct shipment id.</p>
 */
@RestController
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class ShippingController {
    private final ShippingFacade shipments;
    private final OrderFacade orders;
    private final RequestContextResolver requestContext;

    @PostMapping("/api/v1/shipments")
    ResponseEntity<ShipmentView> createShipment(@AuthenticationPrincipal Jwt jwt,
            @RequestBody ShipmentWrite request) {
        if (request.carrier() != null && (request.carrierOverrideReason() == null
                || request.carrierOverrideReason().isBlank())) {
            throw new IllegalArgumentException("carrierOverrideReason is required when carrier is specified.");
        }
        if (request.lineIds() != null && !request.lineIds().isEmpty()) {
            throw new IllegalArgumentException("Split shipments are not available for this order yet.");
        }
        var context = requestContext.resolve(jwt);
        OrderView order = orders.getOrder(context.caller(), request.orderId());
        if (order.shippingAddress() == null) throw new IllegalArgumentException("Order has no shipping address.");
        ShipmentView shipment = shipments.createShipment(context.caller(), order.id(), request.carrier(),
            order.shippingAddress().postalCode(), order.shippingAddress().countryCode(), context.correlationId());
        return ResponseEntity.created(URI.create("/api/v1/shipments/" + shipment.id())).body(shipment);
    }

    @GetMapping("/api/v1/orders/{orderId}/tracking")
    OrderTracking trackOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        var caller = requestContext.resolve(jwt).caller();
        OrderView order = orders.getOrder(caller, orderId);
        return new OrderTracking(order.id(), order.orderNumber(), order.status(), shipments.listShipments(caller, orderId));
    }

    @GetMapping("/api/v1/orders/{orderId}/shipments")
    List<ShipmentView> listOrderShipments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        var caller = requestContext.resolve(jwt).caller();
        orders.getOrder(caller, orderId);
        return shipments.listShipments(caller, orderId);
    }

    @GetMapping("/api/v1/shipments/{shipmentId}")
    ShipmentView getShipment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID shipmentId) {
        var caller = requestContext.resolve(jwt).caller();
        ShipmentView shipment = shipments.getShipment(caller, shipmentId);
        orders.getOrder(caller, shipment.orderId());
        return shipment;
    }

    @GetMapping("/api/v1/shipments/{shipmentId}/tracking-events")
    PageEnvelope<ShipmentView.TrackingEventView> listTrackingEvents(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID shipmentId) {
        ShipmentView shipment = getShipment(jwt, shipmentId);
        return new PageEnvelope<>(shipment.trackingEvents(),
            new Page(shipment.trackingEvents().size(), null, null));
    }

    record OrderTracking(UUID orderId, String orderNumber, String status, List<ShipmentView> shipments) {
        OrderTracking { shipments = List.copyOf(shipments); }
    }

    record ShipmentWrite(UUID orderId, String carrier, String carrierOverrideReason, List<UUID> lineIds) { }
}
