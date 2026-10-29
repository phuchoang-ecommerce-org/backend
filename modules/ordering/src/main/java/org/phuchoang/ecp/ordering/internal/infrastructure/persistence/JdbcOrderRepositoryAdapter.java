package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Persists the Draft-to-Order aggregate transition over the already-frozen checkout row and its owned lines. */
@Repository
@RequiredArgsConstructor
class JdbcOrderRepositoryAdapter implements OrderRepository {
    private final JdbcClient jdbc;

    @Override
    public Optional<Order> findById(UUID id) {
        return jdbc.sql("SELECT * FROM ordering_order WHERE id = :id").param("id", id)
            .query((rs, row) -> new Order(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                rs.getString("order_number"), lines(id), address(rs, "shipping"), address(rs, "billing"),
                new ShippingOption(rs.getString("checkout_shipping_option_id"), rs.getString("checkout_shipping_carrier"),
                    rs.getString("checkout_shipping_service"), new Money(rs.getBigDecimal("shipping_fee_amount"),
                        rs.getString("currency")), rs.getTimestamp("checkout_estimated_delivery_from") == null ? null :
                        rs.getTimestamp("checkout_estimated_delivery_from").toInstant(),
                    rs.getTimestamp("checkout_estimated_delivery_to") == null ? null :
                        rs.getTimestamp("checkout_estimated_delivery_to").toInstant()),
                rs.getObject("promotion_id", UUID.class), rs.getString("promotion_code"),
                new Money(rs.getBigDecimal("subtotal_amount"), rs.getString("currency")),
                new Money(rs.getBigDecimal("discount_amount"), rs.getString("currency")),
                new Money(rs.getBigDecimal("total_amount"), rs.getString("currency")), reservations(id),
                OrderStatus.valueOf(rs.getString("status")), instant(rs, "placed_at"), instant(rs, "created_at"),
                rs.getLong("version"))).optional();
    }

    @Override
    public OrderRepository.OrderPage findPlacedByCustomerId(UUID customerId, String cursor, int size) {
        Cursor after = cursor == null ? null : decode(cursor);
        String sql = "SELECT id, placed_at FROM ordering_order WHERE customer_id = :customerId AND status <> 'DRAFT' "
            + (after == null ? "" : "AND (placed_at, id) < (:placedAt, :id) ")
            + "ORDER BY placed_at DESC NULLS LAST, id DESC LIMIT :limit";
        var query = jdbc.sql(sql).param("customerId", customerId).param("limit", size + 1);
        if (after != null) query = query.param("placedAt", after.placedAt()).param("id", after.id());
        List<Cursor> rows = query.query((rs, row) -> new Cursor(rs.getTimestamp("placed_at").toInstant(),
            rs.getObject("id", UUID.class))).list();
        boolean hasNext = rows.size() > size;
        List<Cursor> page = hasNext ? rows.subList(0, size) : rows;
        String next = hasNext ? encode(page.getLast()) : null;
        return new OrderRepository.OrderPage(page.stream().flatMap(item -> findById(item.id()).stream()).toList(), next);
    }

    @Override public Order save(Order order) {
        int changed = jdbc.sql("""
            UPDATE ordering_order SET status = :status, discount_amount = :discount, total_amount = :total,
                placed_at = now(), checkout_confirmation_token = NULL, updated_at = now(), version = version + 1
            WHERE id = :id AND status = 'DRAFT'
            """).param("status", order.status().name()).param("discount", order.discount().amount())
            .param("total", order.total().amount()).param("id", order.id()).update();
        if (changed != 1) throw new IllegalStateException("Checkout no longer has a Draft order to place.");
        for (var reservation : order.reservations()) {
            jdbc.sql("""
                INSERT INTO ordering_order_line_reservation (id, order_line_id, stock_item_id, stock_reservation_id,
                    warehouse_id, quantity) VALUES (:id, :lineId, :stockItemId, :reservationId, :warehouseId, :quantity)
                """).param("id", UUID.randomUUID()).param("lineId", reservation.orderLineId())
                .param("stockItemId", reservation.stockItemId()).param("reservationId", reservation.reservationId())
                .param("warehouseId", reservation.warehouseId()).param("quantity", reservation.quantity()).update();
        }
        return order;
    }

    @Override
    public Order saveTransition(Order order) {
        int changed = jdbc.sql("""
            UPDATE ordering_order SET status = :status, updated_at = now(), version = version + 1
            WHERE id = :id AND version = :version
            """).param("status", order.status().name()).param("id", order.id()).param("version", order.version()).update();
        if (changed != 1) throw new IllegalStateException("Order changed concurrently; reload before retrying.");
        return new Order(order.id(), order.customerId(), order.orderNumber(), order.lines(), order.shippingAddress(),
            order.billingAddress(), order.shippingOption(), order.promotionId(), order.promotionCode(), order.subtotal(),
            order.discount(), order.total(), order.reservations(), order.status(), order.placedAt(), order.createdAt(),
            order.version() + 1);
    }

    private List<CheckoutLine> lines(UUID orderId) {
        return jdbc.sql("SELECT * FROM ordering_order_line WHERE order_id = :orderId ORDER BY id").param("orderId", orderId)
            .query((rs, row) -> new CheckoutLine(rs.getObject("id", UUID.class), rs.getObject("variant_id", UUID.class),
                rs.getString("sku"), rs.getString("product_name"), rs.getString("variant_name"), rs.getInt("quantity"),
                new Money(rs.getBigDecimal("unit_price_at_order_amount"), rs.getString("unit_price_at_order_currency")))).list();
    }

    private List<OrderReservation> reservations(UUID orderId) {
        return jdbc.sql("""
            SELECT r.order_line_id, r.stock_item_id, r.warehouse_id, r.stock_reservation_id, r.quantity
            FROM ordering_order_line_reservation r JOIN ordering_order_line l ON l.id = r.order_line_id
            WHERE l.order_id = :orderId ORDER BY r.id
            """).param("orderId", orderId).query((rs, row) -> new OrderReservation(
                rs.getObject("order_line_id", UUID.class), rs.getObject("stock_item_id", UUID.class),
                rs.getObject("warehouse_id", UUID.class), rs.getObject("stock_reservation_id", UUID.class),
                rs.getInt("quantity"))).list();
    }

    private static CheckoutAddress address(java.sql.ResultSet rs, String prefix) throws java.sql.SQLException {
        return new CheckoutAddress(null, rs.getString(prefix + "_recipient_name"), rs.getString(prefix + "_line1"),
            rs.getString(prefix + "_line2"), rs.getString(prefix + "_city"), rs.getString(prefix + "_region"),
            rs.getString(prefix + "_postal_code"), rs.getString(prefix + "_country_code"), null);
    }

    private static java.time.Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static String encode(Cursor cursor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            (cursor.placedAt() + "|" + cursor.id()).getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(String value) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", 2);
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid order cursor.", exception);
        }
    }

    private record Cursor(Instant placedAt, UUID id) { }
}
