package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.repository.CheckoutRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** PostgreSQL adapter for the checkout aggregate. */
@Repository
@lombok.RequiredArgsConstructor(access = lombok.AccessLevel.PACKAGE)
class JpaCheckoutRepositoryAdapter implements CheckoutRepository {
    private final CheckoutJpaRepository checkouts;
    private final CheckoutJpaMapper mapper;
    private final Clock clock;

    @Override public Optional<Checkout> findById(UUID id) { return checkouts.findById(id).map(mapper::toDomain); }

    @Override public Optional<Checkout> findDraftByCustomerId(UUID customerId) {
        return checkouts.findByCustomerIdAndStatus(customerId, "DRAFT").map(mapper::toDomain);
    }

    @Override public Checkout save(Checkout checkout) {
        Instant now = clock.instant();
        CheckoutEntity entity = checkouts.findById(checkout.id()).orElse(null);
        if (entity == null) entity = mapper.toEntity(checkout, now);
        else mapper.updateEntity(checkout, entity, now);
        return mapper.toDomain(checkouts.saveAndFlush(entity));
    }
}
