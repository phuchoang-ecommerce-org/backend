package org.phuchoang.ecp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.phuchoang.ecp.ordering.api.OrderFacade;
import org.phuchoang.ecp.ordering.api.PlaceOrderRequest;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;
import org.phuchoang.ecp.ordering.internal.domain.repository.CheckoutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** BR-ORD-02/03: placement and inventory reservation commit once per idempotency key. */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "spring.flyway.default-schema=order_placement_l4",
    "spring.flyway.schemas=order_placement_l4",
    "spring.datasource.hikari.schema=order_placement_l4",
    "spring.datasource.hikari.connection-init-sql=SET search_path TO order_placement_l4",
    "spring.jpa.properties.hibernate.default_schema=order_placement_l4",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false",
    "ecp.outbox.poll-delay-ms=600000"
})
@Import(TestcontainersConfiguration.class)
class OrderPlacementIT {

    @Autowired private OrderFacade orders;
    @Autowired private CheckoutRepository checkouts;
    @Autowired private StockItemRepository stockItems;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void confirmedCheckoutPlacesOneOrderAndOneStockReservationOnRetry() {
        UUID warehouseId = UUID.randomUUID();
        UUID stockItemId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID checkoutId = UUID.randomUUID();
        String sku = "ORD-" + UUID.randomUUID();
        String confirmationToken = "confirm-" + UUID.randomUUID();
        jdbc.update("insert into order_placement_l4.inventory_warehouse (id, code, name, country_code) values (?, ?, ?, ?)",
            warehouseId, "ORD-" + warehouseId.toString().substring(0, 8), "Order test warehouse", "VN");
        CheckoutAddress address = new CheckoutAddress("Home", "Customer", "1 Main Street", null,
            "Ho Chi Minh City", null, "70000", "VN", null);
        CheckoutLine line = new CheckoutLine(UUID.randomUUID(), stockItemId, sku, "Test product", "Standard", 2,
            new Money(new BigDecimal("10.0000"), "USD"));
        ShippingOption shipping = new ShippingOption("standard", "TEST", "Standard",
            new Money(new BigDecimal("2.0000"), "USD"), Instant.parse("2026-10-24T00:00:00Z"),
            Instant.parse("2026-10-25T00:00:00Z"));
        Checkout ready = Checkout.start(checkoutId, customerId, "ORD-" + UUID.randomUUID(), List.of(line))
            .setShippingAddress(address, List.of(shipping))
            .setBillingAddress(address)
            .selectShippingOption(shipping)
            .refresh(List.of(line), confirmationToken);
        transactions.executeWithoutResult(status -> {
            stockItems.saveAll(List.of(StockItem.open(stockItemId, sku, warehouseId, 5)));
            ready.save(checkouts);
        });
        IdentityActor customer = new IdentityActor(customerId, Set.of("CUSTOMER"));
        String idempotencyKey = "order-" + UUID.randomUUID();
        PlaceOrderRequest request = new PlaceOrderRequest(checkoutId, confirmationToken);

        var first = orders.placeOrder(customer, request, idempotencyKey, UUID.randomUUID());
        var retry = orders.placeOrder(customer, request, idempotencyKey, UUID.randomUUID());

        assertThat(first.orderId()).isEqualTo(checkoutId);
        assertThat(first.duplicate()).isFalse();
        assertThat(retry.orderId()).isEqualTo(first.orderId());
        assertThat(retry.duplicate()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from order_placement_l4.ordering_order where id = ? "
            + "and status = 'PENDING_PAYMENT'", Integer.class, checkoutId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from order_placement_l4.inventory_stock_reservation "
            + "where order_id = ?", Integer.class, checkoutId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select quantity_reserved from order_placement_l4.inventory_stock_item "
            + "where id = ?", Integer.class, stockItemId)).isEqualTo(2);
    }
}
