package org.phuchoang.ecp.ordering.internal.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void lifecycleTransitionsAreCentralizedAndRejectIllegalEdges() {
        Order order = Order.place(readyCheckout(), List.of(new OrderReservation(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), 1)), Money.zero("USD"));

        assertThat(order.transitionTo(OrderStatus.PAID).status()).isEqualTo(OrderStatus.PAID);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PACKED))
            .isInstanceOf(Order.IllegalStatusTransition.class)
            .satisfies(error -> {
                Order.IllegalStatusTransition transition = (Order.IllegalStatusTransition) error;
                assertThat(transition.current()).isEqualTo(OrderStatus.PENDING_PAYMENT);
                assertThat(transition.available()).containsExactly(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED,
                    OrderStatus.CANCELLED);
            });
    }

    @Test
    void lifecycleGraphContainsEveryDocumentedEdgeAndNoShortcut() {
        assertThat(OrderStateMachine.availableFrom(OrderStatus.DRAFT))
            .containsExactly(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.PAID))
            .containsExactly(OrderStatus.PROCESSING, OrderStatus.CANCELLED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.PROCESSING))
            .containsExactly(OrderStatus.PACKED, OrderStatus.CANCELLED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.PACKED)).containsExactly(OrderStatus.SHIPPING);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.SHIPPING)).containsExactly(OrderStatus.DELIVERED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.DELIVERED))
            .containsExactly(OrderStatus.COMPLETED, OrderStatus.RETURNED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.CANCELLED)).containsExactly(OrderStatus.REFUNDED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.RETURNED)).containsExactly(OrderStatus.REFUNDED);
        assertThat(OrderStateMachine.availableFrom(OrderStatus.COMPLETED)).isEmpty();
        assertThat(OrderStateMachine.availableFrom(OrderStatus.REFUNDED)).isEmpty();
        assertThat(OrderStateMachine.permits(OrderStatus.PENDING_PAYMENT, OrderStatus.PACKED)).isFalse();
    }

    private static Checkout readyCheckout() {
        CheckoutAddress address = new CheckoutAddress("Home", "Customer", "1 Main St", null, "HCMC", null,
            "70000", "VN", null);
        return new Checkout(UUID.randomUUID(), UUID.randomUUID(), "DRAFT-1", List.of(line()), address, address,
            new ShippingOption("standard", "PROVISIONAL", "Standard", new Money(new BigDecimal("5.0000"), "USD"),
                null, null), null, null, Money.zero("USD"), "confirmed", 0, null);
    }

    private static CheckoutLine line() {
        return new CheckoutLine(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Product", "Variant", 1,
            new Money(new BigDecimal("8.0000"), "USD"));
    }
}
