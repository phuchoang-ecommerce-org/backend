package org.phuchoang.ecp.cart.internal.application.cart.expiry;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartStatus;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;
import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartExpiryServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartExpiryDeferral deferral = mock(CartExpiryDeferral.class);
    private final Instant now = Instant.parse("2026-10-10T00:00:00Z");
    private final CartExpiryService expiry = new CartExpiryService(
        new CartCommandService(carts, Duration.ofDays(7), Duration.ofDays(30)), deferral, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void expiresOnlyDueCartsThatAreNotInActiveCheckoutOrHoldingADraftOrder() {
        Cart expirable = Cart.guest(UUID.randomUUID(), "guest-a", now.minusSeconds(600), now.minusSeconds(1));
        Cart deferred = Cart.guest(UUID.randomUUID(), "guest-b", now.minusSeconds(600), now.minusSeconds(1));
        when(carts.findExpiredActiveAt(now, 100)).thenReturn(List.of(expirable, deferred));
        when(deferral.hasActiveCheckoutOrDraftOrder(deferred.id())).thenReturn(true);

        expiry.expireDueCarts();

        verify(carts).save(eq(expirable.expire()));
        verify(carts, never()).save(eq(deferred.expire()));
    }

    @Test
    void aRepositoryFailureEscapesWithoutMarkingFurtherCartsExpiredSoTheNextRunCanRetry() {
        Cart first = Cart.guest(UUID.randomUUID(), "guest-a", now.minusSeconds(600), now.minusSeconds(1));
        when(carts.findExpiredActiveAt(now, 100)).thenReturn(List.of(first));
        when(carts.save(any())).thenThrow(new IllegalStateException("database unavailable"));

        org.assertj.core.api.Assertions.assertThatThrownBy(expiry::expireDueCarts).isInstanceOf(IllegalStateException.class);
        verify(carts).save(eq(first.expire()));
    }
}
