package org.phuchoang.ecp.payment.internal.application.authorise.command;

import java.net.URI;
import org.phuchoang.ecp.payment.internal.domain.model.Payment;
import org.phuchoang.ecp.payment.internal.domain.model.PaymentAttempt;

/** ACL port: provider terminology and SDKs do not enter the domain. */
public interface PaymentGateway {
    Result authoriseAndCapture(Payment payment, PaymentAttempt attempt, URI returnUrl);
    record Result(Kind kind, String providerReference, URI redirectUrl) {
        public enum Kind { CAPTURED, DECLINED, PENDING }
    }
}
