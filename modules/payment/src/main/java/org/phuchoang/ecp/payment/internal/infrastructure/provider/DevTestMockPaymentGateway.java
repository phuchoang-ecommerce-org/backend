package org.phuchoang.ecp.payment.internal.infrastructure.provider;

import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentGateway;
import org.phuchoang.ecp.payment.internal.application.callback.command.ProviderNotification;
import org.phuchoang.ecp.payment.internal.application.callback.command.ReceivePaymentProviderNotificationService;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Development/test-only provider simulator. The initiation service has already
 * committed the Pending attempt when this adapter emits its signed callback.
 * There is no production activation path and no HTTP endpoint for this helper.
 */
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
class DevTestMockPaymentGateway implements PaymentGateway {
    private final ReceivePaymentProviderNotificationService callbacks;
    private final Clock clock;
    @Value("${ecp.payment.provider-webhook-secret:}")
    private String webhookSecret;

    @Override
    public Result authoriseAndCapture(Payment payment, PaymentAttempt attempt, URI returnUrl) {
        var notification = new ProviderNotification(attempt.providerReference(), "SUCCEEDED", attempt.amount().amount(),
            attempt.amount().currency(), "mock-" + attempt.id(), clock.instant(), Map.of("provider", "dev-test-mock"));
        callbacks.receive(notification, HmacProviderNotificationAuthenticator.signature(notification, webhookSecret),
            UUID.randomUUID());
        return new Result(Result.Kind.PENDING, attempt.providerReference(), null);
    }
}
