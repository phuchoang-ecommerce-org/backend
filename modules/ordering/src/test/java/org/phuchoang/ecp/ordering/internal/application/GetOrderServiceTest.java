package org.phuchoang.ecp.ordering.internal.application;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetOrderServiceTest {
    private final IdentityAuthorization authorization = mock(IdentityAuthorization.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final GetOrderService service = new GetOrderService(authorization, orders);

    @Test
    void hidesAnotherCustomersOrderAsNotFound() {
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));
        Order order = order(UUID.randomUUID());
        when(orders.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.get(caller, order.id()))
            .isInstanceOf(ApplicationException.class)
            .hasMessage("Order not found.");
    }

    private static Order order(UUID customerId) {
        CheckoutAddress address = new CheckoutAddress("Home", "Customer", "1 Main St", null, "HCMC", null,
            "70000", "VN", null);
        CheckoutLine line = new CheckoutLine(UUID.randomUUID(), UUID.randomUUID(), "SKU-1", "Product", null, 1,
            new Money(new BigDecimal("8.0000"), "USD"));
        return new Order(UUID.randomUUID(), customerId, "ORD-1", List.of(line), address, address,
            new ShippingOption("standard", "Carrier", "Standard", new Money(new BigDecimal("5.0000"), "USD"),
                null, null), null, null, line.lineTotal(), Money.zero("USD"),
            new Money(new BigDecimal("13.0000"), "USD"), List.of(new OrderReservation(line.id(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 1)), OrderStatus.PENDING_PAYMENT, null, null, 0);
    }
}
