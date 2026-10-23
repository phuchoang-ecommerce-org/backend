package org.phuchoang.ecp;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.api.OrderFulfilmentFacade;
import org.phuchoang.ecp.ordering.api.OrderFulfilmentView;
import org.phuchoang.ecp.shipping.api.ShippingFacade;
import org.phuchoang.ecp.shipping.internal.application.carrier.CarrierGateway;
import org.phuchoang.ecp.shipping.internal.application.create.command.CreateShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** UC-SHP-03: only accepted external bookings become durable shipments. */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "spring.flyway.default-schema=shipping_booking_l4",
    "spring.flyway.schemas=shipping_booking_l4",
    "spring.datasource.hikari.schema=shipping_booking_l4",
    "spring.datasource.hikari.connection-init-sql=SET search_path TO shipping_booking_l4",
    "spring.jpa.properties.hibernate.default_schema=shipping_booking_l4",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false",
    "ecp.outbox.poll-delay-ms=600000"
})
@Import(TestcontainersConfiguration.class)
class ShippingBookingIT {

    @Autowired private ShippingFacade shipping;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private OrderFulfilmentFacade orders;
    @MockitoBean private CarrierGateway carrier;

    @Test
    void persistsAcceptedBookingAndRejectsDeclinedBookingWithoutShipment() {
        IdentityActor staff = new IdentityActor(UUID.randomUUID(), Set.of("STAFF"));
        UUID acceptedOrder = UUID.randomUUID();
        UUID declinedOrder = UUID.randomUUID();
        when(orders.getOrderForFulfilment(any())).thenAnswer(invocation ->
            new OrderFulfilmentView(invocation.getArgument(0), UUID.randomUUID(), "PACKED"));
        when(carrier.book(eq(acceptedOrder), any(), any(), any()))
            .thenReturn(CarrierGateway.Booking.accepted("SHIP-" + UUID.randomUUID()));
        when(carrier.book(eq(declinedOrder), any(), any(), any()))
            .thenReturn(CarrierGateway.Booking.rejected("Carrier unavailable"));

        var created = shipping.createShipment(staff, acceptedOrder, "TEST", "70000", "VN", UUID.randomUUID());

        assertThat(shipping.getShipment(staff, created.id()).trackingReference())
            .isEqualTo(created.trackingReference());
        assertThat(jdbc.queryForObject("select count(*) from shipping_booking_l4.shipping_shipment where order_id = ?",
            Integer.class, acceptedOrder)).isEqualTo(1);
        assertThatThrownBy(() -> shipping.createShipment(staff, declinedOrder, "TEST", "70000", "VN",
            UUID.randomUUID())).isInstanceOf(CreateShipmentService.CarrierDeclined.class);
        assertThat(jdbc.queryForObject("select count(*) from shipping_booking_l4.shipping_shipment where order_id = ?",
            Integer.class, declinedOrder)).isZero();
    }
}
