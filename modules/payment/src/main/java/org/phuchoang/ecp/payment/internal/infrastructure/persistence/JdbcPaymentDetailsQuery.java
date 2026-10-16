package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.application.get.query.PaymentDetails;
import org.phuchoang.ecp.payment.internal.application.get.query.PaymentDetailsQuery;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC read adapter: no aggregate is reconstructed for this observational query. */
@Repository
@RequiredArgsConstructor
class JdbcPaymentDetailsQuery implements PaymentDetailsQuery {
    private final JdbcClient jdbc;

    @Override
    public Optional<PaymentDetails> findByOrderId(UUID orderId) {
        return jdbc.sql("SELECT id, order_id, customer_id, method, authorised_amount, currency, status "
                + "FROM payment_payment WHERE order_id = :orderId")
            .param("orderId", orderId)
            .query((rs, ignored) -> new PaymentDetails(rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class), rs.getObject("customer_id", UUID.class), rs.getString("method"),
                rs.getBigDecimal("authorised_amount"), rs.getString("currency"), rs.getString("status")))
            .optional();
    }
}
