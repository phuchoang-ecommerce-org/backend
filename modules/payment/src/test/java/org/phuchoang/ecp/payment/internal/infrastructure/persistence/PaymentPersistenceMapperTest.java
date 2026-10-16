package org.phuchoang.ecp.payment.internal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.payment.internal.domain.model.AttemptOutcome;
import org.phuchoang.ecp.payment.internal.domain.model.Money;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentMethod;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentStatus;

class PaymentPersistenceMapperTest {

    private final PaymentPersistenceMapper mapper = Mappers.getMapper(PaymentPersistenceMapper.class);

    @Test
    void maps_the_local_persistence_rows_without_leaking_them_into_the_domain() {
        UUID paymentId = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-16T10:00:00Z");
        PaymentAttempt attempt = new PaymentAttempt(UUID.randomUUID(), "idem-1", 1,
            new Money(new BigDecimal("12.50"), "USD"), AttemptOutcome.PENDING, "provider-1", null, at, null);
        Payment payment = new Payment(paymentId, UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.CARD,
            new Money(new BigDecimal("12.50"), "USD"), PaymentStatus.PENDING, List.of(attempt), at, 4);

        PaymentRow row = mapper.toRow(payment);
        Payment restored = mapper.toDomain(row, List.of(mapper.toDomain(mapper.toRow(attempt, paymentId))));

        assertThat(row.authorisedAmount()).isEqualByComparingTo("12.50");
        assertThat(restored).isEqualTo(payment);
    }
}
