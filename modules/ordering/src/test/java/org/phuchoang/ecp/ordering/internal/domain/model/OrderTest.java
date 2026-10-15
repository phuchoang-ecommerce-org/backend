package org.phuchoang.ecp.ordering.internal.domain.model;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderTest {

    @Test
    void confirmedCheckoutBecomesPendingPaymentOrderWithFrozenAmounts() {
        Checkout checkout = readyCheckout();

        Order order = Order.place(checkout, List.of(new OrderReservation(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), 1)), new Money(new BigDecimal("2.0000"), "USD"));

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.total().amount()).isEqualByComparingTo("11.0000");
        assertThat(order.lines()).hasSize(1);
    }

    @Test
    void orderCannotBePlacedFromAnUnconfirmedOrIncompleteCheckout() {
        Checkout incomplete = Checkout.start(UUID.randomUUID(), UUID.randomUUID(), "DRAFT-1", List.of(line()));

        assertThatThrownBy(() -> Order.place(incomplete, List.of(), Money.zero("USD")))
            .isInstanceOf(Order.NotReadyForPlacement.class);
    }

    @Test
    void savesItsCompleteAggregateThroughItsDomainRepository() {
        OrderRepository orders = mock(OrderRepository.class);
        Order order = Order.place(readyCheckout(), List.of(new OrderReservation(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), 1)), Money.zero("USD"));
        when(orders.save(order)).thenReturn(order);

        assertThat(order.save(orders)).isSameAs(order);
        verify(orders).save(order);
    }

    private static Checkout readyCheckout() {
        CheckoutAddress address = new CheckoutAddress("Home", "Customer", "1 Main St", null, "HCMC", null,
            "70000", "VN", null);
        return new Checkout(UUID.randomUUID(), UUID.randomUUID(), "DRAFT-1", List.of(line()), address, address,
            new ShippingOption("standard", "PROVISIONAL", "Standard", new Money(new BigDecimal("5.0000"), "USD"),
                null, null), null, null, Money.zero("USD"), "confirmed", 0);
    }

    private static CheckoutLine line() {
        return new CheckoutLine(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Product", "Variant", 1,
            new Money(new BigDecimal("8.0000"), "USD"));
    }
}
