package org.phuchoang.ecp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.payment.api.PaymentFacade;
import org.phuchoang.ecp.payment.api.PaymentOutcome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = PaymentReconciliationIT.TestApplication.class, properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "ecp.payment.provider-webhook-secret=integration-webhook-secret",
    "spring.flyway.default-schema=payment_reconciliation_l4",
    "spring.flyway.schemas=payment_reconciliation_l4",
    "spring.datasource.hikari.schema=payment_reconciliation_l4",
    "spring.jpa.properties.hibernate.default_schema=payment_reconciliation_l4",
    "spring.jpa.hibernate.ddl-auto=none",
    "management.endpoint.health.validate-group-membership=false",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("payment-reconciliation-test")
class PaymentReconciliationIT {
    @Configuration(proxyBeanMethods = false)
    @Profile("payment-reconciliation-test")
    @EnableAutoConfiguration
    @ComponentScan("org.phuchoang.ecp.payment")
    static class TestApplication {
        @Bean java.time.Clock clock() { return java.time.Clock.systemUTC(); }
        @Bean org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization authorization() {
            return (caller, operation) -> {
                if (!caller.roles().contains("CUSTOMER_SUPPORT") && !caller.roles().contains("ADMINISTRATOR"))
                    throw new org.phuchoang.ecp.identity.api.error.DomainException(
                        org.phuchoang.ecp.identity.api.error.GenErrorCode.FORBIDDEN, "Forbidden.");
            };
        }
    }
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PaymentFacade payments;

    @Test
    void queuePagesUnresolvedResultsAndRestrictsAccess() {
        jdbc.update("delete from payment_reconciliation_l4.payment_unmatched_notification");
        UUID newer = UUID.randomUUID();
        UUID older = UUID.randomUUID();
        insert(newer, "newer", "2026-10-17T10:00:00Z", null);
        insert(older, "older", "2026-10-17T09:00:00Z", null);
        insert(UUID.randomUUID(), "resolved", "2026-10-17T11:00:00Z", Instant.parse("2026-10-17T12:00:00Z"));
        IdentityActor support = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER_SUPPORT"));

        var first = payments.listUnmatchedPayments(support, false, null, 1);
        assertThat(first.items()).extracting(PaymentFacade.UnmatchedPayment::id).containsExactly(newer);
        assertThat(first.items().getFirst().amount()).isEqualByComparingTo(new BigDecimal("10.1250"));
        assertThat(first.nextCursor()).isNotBlank();
        var second = payments.listUnmatchedPayments(support, false, first.nextCursor(), 1);
        assertThat(second.items()).extracting(PaymentFacade.UnmatchedPayment::id).containsExactly(older);
        assertThat(second.nextCursor()).isNull();
        assertThatThrownBy(() -> payments.listUnmatchedPayments(support, true, first.nextCursor(), 1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> payments.listUnmatchedPayments(support, false, first.nextCursor() + "x", 1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> payments.listUnmatchedPayments(
            new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER")), false, null, 20))
            .isInstanceOf(org.phuchoang.ecp.identity.api.error.DomainException.class);
    }

    @Test
    void authenticatedUnmatchedCallbackEntersTheQueueOnlyOnce() throws Exception {
        jdbc.update("delete from payment_reconciliation_l4.payment_unmatched_notification");
        String reference = "unknown-" + UUID.randomUUID();
        String event = "event-" + UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.1250");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("integration-webhook-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = java.util.HexFormat.of().formatHex(mac.doFinal(
            (reference + "|SUCCEEDED|" + amount + "|" + event).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        UUID correlation = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T10:00:00Z");
        assertThat(payments.receivePaymentProviderNotification(reference, PaymentOutcome.SUCCEEDED, amount, "USD",
            event, now, java.util.Map.of(), signature, correlation)).isEqualTo(PaymentFacade.CallbackResult.UNMATCHED);
        assertThat(payments.receivePaymentProviderNotification(reference, PaymentOutcome.SUCCEEDED, amount, "USD",
            event, now, java.util.Map.of(), signature, correlation)).isEqualTo(PaymentFacade.CallbackResult.DUPLICATE);

        var page = payments.listUnmatchedPayments(new IdentityActor(UUID.randomUUID(), Set.of("ADMINISTRATOR")), false, null, 20);
        assertThat(page.items()).extracting(PaymentFacade.UnmatchedPayment::providerReference).containsExactly(reference);
    }

    private void insert(UUID id, String reference, String received, Instant resolved) {
        jdbc.update("insert into payment_reconciliation_l4.payment_unmatched_notification "
            + "(id, provider_reference, outcome, amount, currency, received_at, resolved_at) "
            + "values (?, ?, 'SUCCEEDED', ?, 'USD', ?, ?)", id, reference, new BigDecimal("10.1250"),
            java.sql.Timestamp.from(Instant.parse(received)), resolved == null ? null : java.sql.Timestamp.from(resolved));
    }
}
