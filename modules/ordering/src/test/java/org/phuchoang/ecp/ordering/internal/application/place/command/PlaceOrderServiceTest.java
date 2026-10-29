package org.phuchoang.ecp.ordering.internal.application.place.command;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;
import org.phuchoang.ecp.ordering.internal.domain.service.CheckoutDomainService;
import org.phuchoang.ecp.ordering.internal.domain.service.PlaceOrderDomainService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class PlaceOrderServiceTest {
    private final IdentityAuthorization authorization = mock(IdentityAuthorization.class);
    private final CheckoutDomainService checkouts = mock(CheckoutDomainService.class);
    private final PlaceOrderDomainService orders = mock(PlaceOrderDomainService.class);
    private final StockReservationPort stock = mock(StockReservationPort.class);
    private final PromotionRedemptionPort promotions = mock(PromotionRedemptionPort.class);
    private final OrderIdempotencyStore idempotency = mock(OrderIdempotencyStore.class);
    private final OrderOutbox outbox = mock(OrderOutbox.class);
    private final PlaceOrderService service = new PlaceOrderService(authorization, checkouts, orders, stock, promotions,
        idempotency, outbox, Clock.fixed(Instant.parse("2026-10-15T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void placesOnlyAfterClaimingTheIdempotencyKeyAndWritesItsOutboxEvent() {
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));
        Checkout checkout = readyCheckout(caller.accountId());
        when(idempotency.find("attempt-1")).thenReturn(java.util.Optional.empty());
        when(idempotency.claim(eq("attempt-1"), eq(caller.accountId()), any(), eq(checkout.id()))).thenReturn(true);
        when(checkouts.requireForPlacement(checkout.id(), caller.accountId())).thenReturn(checkout);
        when(stock.reserve(any(), any(), any())).thenReturn(List.of(new StockReservationPort.Reservation(checkout.lines().getFirst().id(),
            checkout.lines().getFirst().variantId(), UUID.randomUUID(), UUID.randomUUID(), 1)));
        Order order = Order.place(checkout, List.of(new org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation(
            checkout.lines().getFirst().id(), checkout.lines().getFirst().variantId(), UUID.randomUUID(), UUID.randomUUID(), 1)), Money.zero("USD"));
        when(orders.place(eq(checkout), any(), any())).thenReturn(order);

        PlaceOrderResult result = service.place(new PlaceOrderCommand(caller, checkout.id(), "confirmed", "attempt-1",
            UUID.randomUUID()));

        assertThat(result).isEqualTo(new PlaceOrderResult(checkout.id(), "PENDING_PAYMENT", false));
        verify(outbox).appendOrderCreated(eq(checkout.id()), any(), eq(caller), any(), contains("PENDING_PAYMENT"));
        verify(promotions, never()).redeem(any(), any(), any(), any());
    }

    @Test
    void rejectsAMissingIdempotencyKeyBeforeAnyCommercialEffect() {
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));

        assertThatThrownBy(() -> service.place(new PlaceOrderCommand(caller, UUID.randomUUID(), "confirmed", null,
            UUID.randomUUID()))).isInstanceOf(ApplicationException.class);

        verifyNoInteractions(checkouts, orders, stock, promotions, idempotency, outbox);
    }

    private static Checkout readyCheckout(UUID customerId) {
        CheckoutAddress address = new CheckoutAddress("Home", "Customer", "1 Main St", null, "HCMC", null,
            "70000", "VN", null);
        return new Checkout(UUID.randomUUID(), customerId, "DRAFT-1", List.of(new CheckoutLine(UUID.randomUUID(),
            UUID.randomUUID(), "SKU-1", "Product", "Variant", 1, new Money(new BigDecimal("8.0000"), "USD"))),
            address, address, new ShippingOption("standard", "PROVISIONAL", "Standard", new Money(new BigDecimal("5"),
                "USD"), null, null), null, null, Money.zero("USD"), "confirmed", 0, null);
    }
}
