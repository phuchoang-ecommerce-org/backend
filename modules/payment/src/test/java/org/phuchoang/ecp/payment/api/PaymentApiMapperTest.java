package org.phuchoang.ecp.payment.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.payment.internal.application.authorise.command.InitiatePaymentResult;

class PaymentApiMapperTest {

    private final PaymentApiMapper mapper = Mappers.getMapper(PaymentApiMapper.class);

    @Test
    void maps_private_application_results_to_the_stable_public_contract() {
        InitiatePaymentResult result = new InitiatePaymentResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "CARD", new BigDecimal("12.50"), "USD", "PENDING", UUID.randomUUID(), 1, "PENDING", "provider-1",
            Instant.parse("2026-10-30T00:00:00Z"), null, URI.create("https://payments.example/continue"), true);

        PaymentFacade.Initiation mapped = mapper.toInitiation(result);

        assertThat(mapped.payment().method()).isEqualTo(PaymentMethod.CARD);
        assertThat(mapped.attempt().outcome()).isEqualTo(PaymentOutcome.PENDING);
        assertThat(mapped.attempt().paymentId()).isEqualTo(result.paymentId());
        assertThat(mapped.attempt().amount()).isEqualByComparingTo("12.50");
        assertThat(mapped.attempt().requestedAt()).isEqualTo(result.requestedAt());
        assertThat(mapped.redirectUrl()).isEqualTo(result.redirectUrl());
    }
}
