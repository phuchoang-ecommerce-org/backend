package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Persists the Draft-to-Order aggregate transition over the already-frozen checkout row and its owned lines. */
@Repository
@RequiredArgsConstructor
class JdbcOrderRepositoryAdapter implements OrderRepository {
    private final JdbcClient jdbc;

    @Override public Optional<Order> findById(UUID id) { return Optional.empty(); }

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
}
