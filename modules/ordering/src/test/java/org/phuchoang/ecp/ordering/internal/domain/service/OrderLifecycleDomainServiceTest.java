package org.phuchoang.ecp.ordering.internal.domain.service;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderStatus;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLifecycleDomainServiceTest {
    @Test
    void persistsTheAggregateOnlyAfterItsLegalTransition() {
        OrderRepository orders = mock(OrderRepository.class);
        Order order = pendingPaymentOrder();
        when(orders.findById(order.id())).thenReturn(Optional.of(order));
        when(orders.saveTransition(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        Order transitioned = new OrderLifecycleDomainService(orders).transition(order.id(), OrderStatus.PAID);

        assertThat(transitioned.status()).isEqualTo(OrderStatus.PAID);
        verify(orders).saveTransition(transitioned);
    }

    private static Order pendingPaymentOrder() {
        CheckoutAddress address = new CheckoutAddress("Home", "Customer", "1 Main St", null, "HCMC", null,
            "70000", "VN", null);
        Checkout checkout = new Checkout(UUID.randomUUID(), UUID.randomUUID(), "ORD-1", List.of(new CheckoutLine(
            UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Product", "Variant", 1,
            new Money(new BigDecimal("8.0000"), "USD"))), address, address,
            new ShippingOption("standard", "PROVISIONAL", "Standard", new Money(new BigDecimal("5.0000"), "USD"),
                null, null), null, null, Money.zero("USD"), "confirmed", 0);
        return Order.place(checkout, List.of(new OrderReservation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), 1)), Money.zero("USD"));
    }
}
