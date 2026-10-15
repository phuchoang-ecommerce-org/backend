package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutJpaMapperTest {
    private final CheckoutJpaMapper mapper = Mappers.getMapper(CheckoutJpaMapper.class);

    @Test
    void mapsTheCompleteCheckoutAggregateAndOwnedLinesWithoutJpaLeakage() {
        Instant persistedAt = Instant.parse("2026-10-15T00:00:00Z");
        // The authoritative ordering table stores the postal snapshot only; label and phone are
        // Identity address-book metadata and have never been persisted on a Draft order.
        CheckoutAddress address = new CheckoutAddress(null, "Ada Lovelace", "1 Analytical Engine Way", null, "London", null,
            "SW1A 1AA", "GB", null);
        Checkout checkout = Checkout.start(UUID.randomUUID(), UUID.randomUUID(), "DRAFT-1", List.of(new CheckoutLine(UUID.randomUUID(),
            UUID.randomUUID(), "SKU-1", "Product", "Variant", 2, new Money(new BigDecimal("12.5000"), "USD"))))
            .setShippingAddress(address, List.of())
            .setBillingAddress(address)
            .selectShippingOption(new ShippingOption("provisional-standard", "PROVISIONAL", "Standard",
                new Money(new BigDecimal("5.0000"), "USD"), persistedAt.plusSeconds(86400), persistedAt.plusSeconds(172800)));

        CheckoutEntity entity = mapper.toEntity(checkout, persistedAt);

        assertThat(entity).extracting(CheckoutEntity::getId, CheckoutEntity::getCustomerId, CheckoutEntity::getOrderNumber,
            CheckoutEntity::getSubtotalAmount, CheckoutEntity::getTotalAmount).containsExactly(checkout.id(), checkout.customerId(),
            checkout.orderNumber(), new BigDecimal("25.0000"), new BigDecimal("30.0000"));
        assertThat(entity.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getCheckout()).isSameAs(entity);
            assertThat(line.getCreatedAt()).isEqualTo(persistedAt);
            assertThat(line.getLineTotalAmount()).isEqualByComparingTo("25.0000");
        });
        assertThat(entity.getCreatedAt()).isEqualTo(persistedAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(persistedAt);
        assertThat(mapper.toDomain(entity)).isEqualTo(checkout);
    }
}
