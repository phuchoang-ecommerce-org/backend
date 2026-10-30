package org.phuchoang.ecp.payment.internal.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotification;

class HmacProviderNotificationAuthenticatorTest {
    private static final String SECRET = "test-webhook-secret";

    @Test
    void binds_every_callback_field_before_it_can_change_a_payment() {
        ProviderNotification notification = new ProviderNotification("ref-1", "SUCCEEDED", new BigDecimal("20.00"),
            "USD", "event-1", Instant.parse("2026-10-30T00:00:00Z"), Map.of("kind", "CAPTURE"));
        var authenticator = new HmacProviderNotificationAuthenticator(SECRET);
        String signature = HmacProviderNotificationAuthenticator.signature(notification, SECRET);

        assertThat(authenticator.authentic(notification, signature)).isTrue();
        assertThat(authenticator.authentic(new ProviderNotification("ref-1", "SUCCEEDED", new BigDecimal("20.00"),
            "VND", "event-1", Instant.parse("2026-10-30T00:00:00Z"), Map.of("kind", "CAPTURE")), signature)).isFalse();
        assertThat(authenticator.authentic(new ProviderNotification("ref-1", "SUCCEEDED", new BigDecimal("20.00"),
            "USD", "event-1", Instant.parse("2026-10-30T00:00:00Z"), Map.of("kind", "REFUND")), signature)).isFalse();
    }
}
