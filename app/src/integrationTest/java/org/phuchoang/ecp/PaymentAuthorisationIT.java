package org.phuchoang.ecp;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.payment.api.PaymentFacade;
import org.phuchoang.ecp.payment.api.PaymentMethod;
import org.phuchoang.ecp.payment.api.PaymentOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** UC-PAY-02 / BR-PAY-01: a durable pending attempt receives a provider outcome once. */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "ecp.payment.provider-webhook-secret=integration-webhook-secret",
    "spring.flyway.default-schema=payment_authorisation_l4",
    "spring.flyway.schemas=payment_authorisation_l4",
    "spring.datasource.hikari.schema=payment_authorisation_l4",
    "spring.datasource.hikari.connection-init-sql=SET search_path TO payment_authorisation_l4",
    "spring.jpa.properties.hibernate.default_schema=payment_authorisation_l4",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false",
    "ecp.outbox.poll-delay-ms=600000"
})
@Import(TestcontainersConfiguration.class)
class PaymentAuthorisationIT {

    @Autowired private PaymentFacade payments;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void pendingAttemptIsPersistedAndAuthenticatedOutcomeAppliesOnlyOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.1250");
        Instant occurredAt = Instant.parse("2026-10-17T10:00:00Z");
        payments.recordOrderCreated(orderId, customerId, amount, "USD", "PENDING_PAYMENT", occurredAt);
        IdentityActor customer = new IdentityActor(customerId, Set.of("CUSTOMER"));

        var initiated = payments.initiatePayment(customer, orderId, PaymentMethod.CARD,
            "pay-" + UUID.randomUUID(), URI.create("https://example.com/return"), UUID.randomUUID());

        assertThat(initiated.accepted()).isTrue();
        assertThat(initiated.attempt().outcome()).isEqualTo(PaymentOutcome.PENDING);
        assertThat(jdbc.queryForObject("select count(*) from payment_authorisation_l4.payment_attempt where payment_id = ?",
            Integer.class, initiated.payment().id())).isEqualTo(1);

        String reference = initiated.attempt().providerReference();
        String eventId = "provider-" + UUID.randomUUID();
        String signature = sign(reference, amount, eventId);
        assertThat(payments.receivePaymentProviderNotification(reference, PaymentOutcome.SUCCEEDED, amount, "USD",
            eventId, occurredAt, Map.of(), signature, UUID.randomUUID()))
            .isEqualTo(PaymentFacade.CallbackResult.APPLIED);
        assertThat(payments.receivePaymentProviderNotification(reference, PaymentOutcome.SUCCEEDED, amount, "USD",
            eventId, occurredAt, Map.of(), signature, UUID.randomUUID()))
            .isEqualTo(PaymentFacade.CallbackResult.DUPLICATE);
        assertThat(payments.getOrderPayment(orderId).status()).isEqualTo("CAPTURED");
        assertThat(jdbc.queryForObject("select count(*) from payment_authorisation_l4.payment_outbox where aggregate_id = ?",
            Integer.class, initiated.payment().id())).isEqualTo(1);
    }

    private static String sign(String reference, BigDecimal amount, String eventId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("integration-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(
            (reference + "|SUCCEEDED|" + amount + "|" + eventId).getBytes(StandardCharsets.UTF_8)));
    }
}
