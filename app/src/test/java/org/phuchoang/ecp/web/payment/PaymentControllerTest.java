package org.phuchoang.ecp.web.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.api.CheckoutFacade;
import org.phuchoang.ecp.payment.api.PaymentFacade;
import org.phuchoang.ecp.payment.api.PaymentOutcome;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {
    @Test
    void unmatchedMoneyIsAStringAmountAndCurrencyObject() {
        PaymentFacade payments = mock(PaymentFacade.class);
        RequestContextResolver context = mock(RequestContextResolver.class);
        IdentityActor support = new IdentityActor(UUID.randomUUID(), java.util.Set.of("CUSTOMER_SUPPORT"));
        when(context.resolve(null)).thenReturn(new RequestContext(UUID.randomUUID(), support));
        var item = new PaymentFacade.UnmatchedPayment(UUID.randomUUID(), "ref", new BigDecimal("10.1250"), "USD",
            "SUCCEEDED", Instant.parse("2026-10-17T00:00:00Z"), null);
        when(payments.listUnmatchedPayments(support, false, null, 20))
            .thenReturn(new PaymentFacade.UnmatchedPaymentPage(List.of(item), null));
        PaymentController controller = new PaymentController(payments, mock(CheckoutFacade.class), context);

        var result = controller.unmatched(null, new MockHttpServletRequest(), false, null, 20);

        assertThat(result.items().getFirst().amount()).isNotInstanceOf(BigDecimal.class)
            .extracting("amount", "currency").containsExactly("10.1250", "USD");
    }

    @Test
    void providerCallbackPropagatesTheEdgeCorrelationId() {
        PaymentFacade payments = mock(PaymentFacade.class);
        RequestContextResolver context = mock(RequestContextResolver.class);
        UUID correlationId = UUID.randomUUID();
        when(context.resolve(null)).thenReturn(new RequestContext(correlationId, IdentityActor.GUEST));
        PaymentController controller = new PaymentController(payments, mock(CheckoutFacade.class), context);
        Instant occurredAt = Instant.parse("2026-10-17T00:00:00Z");
        var notification = new PaymentController.ProviderNotificationWrite("ref", PaymentOutcome.SUCCEEDED,
            new BigDecimal("20.0000"), "USD", "event", occurredAt, Map.of());

        controller.receive(notification, "signature");

        verify(payments).receivePaymentProviderNotification("ref", PaymentOutcome.SUCCEEDED,
            new BigDecimal("20.0000"), "USD", "event", occurredAt, Map.of(), "signature", correlationId);
    }
}
