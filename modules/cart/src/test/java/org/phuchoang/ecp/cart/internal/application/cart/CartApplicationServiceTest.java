package org.phuchoang.ecp.cart.internal.application.cart;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.internal.application.cart.command.AddCartLineCommand;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartQuantityExceededException;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartView;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.repository.CartRepository;
import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;
import org.phuchoang.ecp.cart.internal.infrastructure.configuration.CartLifetimeProperties;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartReadQuery;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartApplicationServiceTest {
    private final UUID variantId = UUID.randomUUID();
    private final InMemoryCarts carts = new InMemoryCarts();
    private final CartApplicationMapper mapper = Mappers.getMapper(CartApplicationMapper.class);
    private final CartApplicationService service = new CartApplicationService(commands(carts), carts, id -> Optional.of(new VariantGateway.Variant(id,
        "SKU-1", "T-Shirt", "Red / M", new BigDecimal("12.50"), "USD", true, 2)),
        Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC), mapper);

    @Test
    void createsAGuestCartAndKeepsTheExistingQuantityWhenAnIncreaseExceedsStock() {
        CartView cart = service.current(IdentityActor.GUEST, "unguessable-token");
        CartView added = service.add(IdentityActor.GUEST, "unguessable-token", cart.id(), new AddCartLineCommand(variantId, 2));

        assertThatThrownBy(() -> service.changeQuantity(IdentityActor.GUEST, "unguessable-token", added.id(),
            added.lines().getFirst().id(), 3)).isInstanceOf(CartQuantityExceededException.class)
            .extracting(exception -> ((CartQuantityExceededException) exception).availableQuantity()).isEqualTo(2);
        assertThat(carts.findById(added.id()).orElseThrow().lines()).singleElement().extracting(line -> line.quantity()).isEqualTo(2);
    }

    @Test
    void projectsAnExistingCartDomainAggregateIntoItsApplicationReadModel() {
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        Cart cart = Cart.guest(UUID.randomUUID(), "unguessable-token", now, now.plus(Duration.ofDays(7)))
            .add(variantId, "SKU-1", 2, now);
        carts.save(cart);

        CartView result = service.current(IdentityActor.GUEST, "unguessable-token");

        assertThat(result).extracting(CartView::id, CartView::status, CartView::subtotal)
            .containsExactly(cart.id(), "ACTIVE", new org.phuchoang.ecp.cart.internal.application.cart.query.MoneyView(new BigDecimal("25.00"), "USD"));
        assertThat(result.lines()).singleElement().extracting(line -> line.id(), line -> line.quantity(), line -> line.productName())
            .containsExactly(cart.lines().getFirst().id(), 2, "T-Shirt");
    }

    @Test
    void readsACartThroughTheApplicationQueryPortWithoutUsingTheAggregateRepository() {
        UUID cartId = UUID.randomUUID();
        CartReadQuery reads = new CartReadQuery() {
            @Override public Optional<CartReadModel> readActiveByCustomerId(UUID customerId) { return Optional.empty(); }
            @Override public Optional<CartReadModel> readActiveByGuestToken(String guestToken) { return Optional.empty(); }
            @Override public Optional<CartReadModel> readById(UUID id) {
                return Optional.of(new CartReadModel(cartId, null, "unguessable-token", "ACTIVE", Instant.parse("2026-10-09T00:00:00Z"),
                    Instant.parse("2026-10-16T00:00:00Z"), List.of()));
            }
        };
        CartRepository forbiddenOnRead = new CartRepository() {
            @Override public Optional<Cart> findById(UUID id) { throw new AssertionError("query used aggregate repository"); }
            @Override public Optional<Cart> findActiveByCustomerId(UUID customerId) { throw new AssertionError("query used aggregate repository"); }
            @Override public Optional<Cart> findActiveByGuestToken(String token) { throw new AssertionError("query used aggregate repository"); }
            @Override public Optional<Cart> findActiveByGuestTokenForUpdate(String token) { throw new AssertionError("query used aggregate repository"); }
            @Override public Optional<Cart> findActiveByCustomerIdForUpdate(UUID customerId) { throw new AssertionError("query used aggregate repository"); }
            @Override public List<Cart> findExpiredActiveAt(Instant now, int limit) { throw new AssertionError("query used aggregate repository"); }
            @Override public Cart save(Cart cart) { throw new AssertionError("query wrote aggregate repository"); }
        };
        CartApplicationService queryService = new CartApplicationService(commands(forbiddenOnRead), reads, id -> Optional.empty(),
            Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC), mapper);

        CartView result = queryService.get(IdentityActor.GUEST, "unguessable-token", cartId);

        assertThat(result.id()).isEqualTo(cartId);
    }

    @Test
    void mergesAGuestCartThroughTheDomainCommandModel() {
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        Cart guest = Cart.guest(UUID.randomUUID(), "unguessable-token", now, now.plus(Duration.ofDays(7)))
            .add(variantId, "SKU-1", 1, now);
        UUID customerId = UUID.randomUUID();
        carts.save(guest);

        assertThat(service.mergeGuestCart(customerId, "unguessable-token").merged()).isTrue();

        assertThat(carts.findById(guest.id()).orElseThrow().status().name()).isEqualTo("MERGED");
        assertThat(carts.findActiveByCustomerId(customerId).orElseThrow().lines())
            .singleElement().extracting(line -> line.variantId()).isEqualTo(variantId);
    }

    private static final class InMemoryCarts implements CartRepository, CartReadQuery {
        private final Map<UUID, Cart> values = new HashMap<>();
        @Override public Optional<Cart> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public Optional<Cart> findActiveByCustomerId(UUID customerId) { return values.values().stream().filter(value -> customerId.equals(value.customerId()) && value.status().name().equals("ACTIVE")).findFirst(); }
        @Override public Optional<Cart> findActiveByGuestToken(String token) { return values.values().stream().filter(value -> token.equals(value.guestToken()) && value.status().name().equals("ACTIVE")).findFirst(); }
        @Override public Optional<Cart> findActiveByGuestTokenForUpdate(String token) { return findActiveByGuestToken(token); }
        @Override public Optional<Cart> findActiveByCustomerIdForUpdate(UUID customerId) { return findActiveByCustomerId(customerId); }
        @Override public List<Cart> findExpiredActiveAt(Instant now, int limit) { return values.values().stream().filter(value -> value.isExpiredAt(now)).limit(limit).toList(); }
        @Override public Cart save(Cart cart) { values.put(cart.id(), cart); return cart; }
        @Override public Optional<CartReadModel> readActiveByCustomerId(UUID customerId) {
            return findActiveByCustomerId(customerId).map(InMemoryCarts::readModel);
        }
        @Override public Optional<CartReadModel> readActiveByGuestToken(String guestToken) {
            return findActiveByGuestToken(guestToken).map(InMemoryCarts::readModel);
        }
        @Override public Optional<CartReadModel> readById(UUID cartId) {
            return findById(cartId).map(InMemoryCarts::readModel);
        }
        private static CartReadModel readModel(Cart cart) {
            return new CartReadModel(cart.id(), cart.customerId(), cart.guestToken(), cart.status().name(), cart.lastActivityAt(), cart.expiresAt(),
                cart.lines().stream().map(line -> new CartLineReadModel(line.id(), line.variantId(), line.sku(), line.quantity(), line.addedAt())).toList());
        }
    }

    private static CartLifetimeProperties lifetimes() {
        return new CartLifetimeProperties(Duration.ofDays(7), Duration.ofDays(30), Duration.ofMinutes(1));
    }

    private static CartCommandService commands(CartRepository carts) {
        CartLifetimeProperties lifetimes = lifetimes();
        return new CartCommandService(carts, lifetimes.guest(), lifetimes.customer());
    }
}
