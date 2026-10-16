package org.phuchoang.ecp.payment.internal.infrastructure.provider;
import java.net.URI;
import org.phuchoang.ecp.payment.internal.application.authorise.command.PaymentGateway;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;
import org.springframework.stereotype.Component;
/** Safe development ACL: production replaces this adapter with a provider integration. */
@Component
class PendingPaymentGateway implements PaymentGateway {
    @Override public Result authoriseAndCapture(Payment payment, PaymentAttempt attempt, URI returnUrl) { return new Result(Result.Kind.PENDING, attempt.providerReference(), returnUrl); }
}
